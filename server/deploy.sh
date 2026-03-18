#!/bin/bash
set -e

echo "=== Mobile Dialer Backend — VPS Deployment ==="
echo ""

# ─── 1. System dependencies ───
echo "[1/7] Installing system dependencies..."
sudo apt update && sudo apt install -y curl git nginx

# Install Node.js 20 if not present
if ! command -v node &> /dev/null; then
  echo "Installing Node.js 20..."
  curl -fsSL https://deb.nodesource.com/setup_20.x | sudo -E bash -
  sudo apt install -y nodejs
fi

# Install PM2 if not present
if ! command -v pm2 &> /dev/null; then
  echo "Installing PM2..."
  sudo npm install -g pm2
fi

echo "Node: $(node -v) | NPM: $(npm -v) | PM2: $(pm2 -v)"

# ─── 2. PostgreSQL ───
echo ""
echo "[2/7] Setting up PostgreSQL..."
if ! command -v psql &> /dev/null; then
  sudo apt install -y postgresql postgresql-contrib
  sudo systemctl enable postgresql
  sudo systemctl start postgresql
fi

# Create database and user (skip if exists)
sudo -u postgres psql -tc "SELECT 1 FROM pg_roles WHERE rolname='dialer_mobile'" | grep -q 1 || \
  sudo -u postgres psql -c "CREATE USER dialer_mobile WITH PASSWORD 'CHANGE_THIS_PASSWORD';"

sudo -u postgres psql -tc "SELECT 1 FROM pg_database WHERE datname='dialer_mobile'" | grep -q 1 || \
  sudo -u postgres psql -c "CREATE DATABASE dialer_mobile OWNER dialer_mobile;"

echo "PostgreSQL ready."

# ─── 3. App directory ───
echo ""
echo "[3/7] Setting up application..."
APP_DIR="/opt/mobile-dialer"
sudo mkdir -p $APP_DIR
sudo chown $USER:$USER $APP_DIR

# Copy files (run this from the server/ directory)
cp -r src package.json package-lock.json $APP_DIR/

# ─── 4. Install dependencies ───
echo ""
echo "[4/7] Installing Node.js dependencies..."
cd $APP_DIR
npm ci --omit=dev

# ─── 5. Environment config ───
echo ""
echo "[5/7] Setting up environment..."
if [ ! -f $APP_DIR/.env ]; then
  echo "⚠️  No .env file found. Creating from template..."
  cp /opt/mobile-dialer/.env.example $APP_DIR/.env 2>/dev/null || cat > $APP_DIR/.env << 'ENVEOF'
PORT=3008
NODE_ENV=production
DATABASE_URL=postgres://dialer_mobile:CHANGE_THIS_PASSWORD@localhost:5432/dialer_mobile
JWT_SECRET=CHANGE_ME_$(openssl rand -hex 32)
JWT_REFRESH_SECRET=CHANGE_ME_$(openssl rand -hex 32)
TWILIO_ACCOUNT_SID=ACxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
TWILIO_AUTH_TOKEN=xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
TWILIO_API_KEY=SKxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
TWILIO_API_SECRET=xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
TWILIO_TWIML_APP_SID=APxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
TWILIO_PHONE_NUMBER=+1xxxxxxxxxx
SERVER_BASE_URL=https://your-domain.com/api
ENVEOF
  echo ""
  echo "⚠️  IMPORTANT: Edit $APP_DIR/.env with your actual credentials before continuing!"
  echo "   nano $APP_DIR/.env"
  echo ""
  echo "After editing .env, re-run this script to continue."
  exit 0
fi

# ─── 6. Run migrations ───
echo ""
echo "[6/7] Running database migrations..."
cd $APP_DIR
node src/db/migrate.js

# ─── 7. Start with PM2 ───
echo ""
echo "[7/7] Starting server with PM2..."
pm2 delete mobile-dialer 2>/dev/null || true
pm2 start src/index.js --name mobile-dialer
pm2 save
pm2 startup | tail -1 | bash 2>/dev/null || true

echo ""
echo "=== Deployment complete! ==="
echo ""
echo "Server running on port 3008"
echo "Check status:  pm2 status"
echo "View logs:     pm2 logs mobile-dialer"
echo ""
echo "── Next steps ──"
echo "1. Set up Nginx reverse proxy (see below)"
echo "2. Set up SSL with: sudo certbot --nginx -d your-domain.com"
echo "3. Update Twilio TwiML App webhook URL to: https://your-domain.com/api/twilio/voice"
echo "4. Create an agent: node -e \"const pool=require('./src/db/pool'); const bcrypt=require('bcryptjs'); (async()=>{const h=await bcrypt.hash('yourpassword',10); await pool.query('INSERT INTO agents(username,password_hash,display_name,twilio_identity) VALUES(\\\$1,\\\$2,\\\$3,\\\$4)',['admin',h,'Admin','agent_admin']); console.log('Agent created'); process.exit();})()\""
echo "5. Point mobile app to: https://your-domain.com/"
echo ""
echo "── Nginx config (paste into /etc/nginx/sites-available/mobile-dialer) ──"
cat << 'NGINX'

server {
    listen 80;
    server_name your-domain.com;

    location / {
        proxy_pass http://127.0.0.1:3008;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}

NGINX
echo ""
echo "Then run:"
echo "  sudo ln -s /etc/nginx/sites-available/mobile-dialer /etc/nginx/sites-enabled/"
echo "  sudo nginx -t && sudo systemctl reload nginx"
echo "  sudo certbot --nginx -d your-domain.com"
