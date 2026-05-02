# Karmic AI — Complete Project

## Code Structure

karmic-ai/
├── frontend/         
│   ├── index.html
│   ├── signup.html
│   ├── dashboard.html
│   ├── admin.html
│   ├── peer.html
│   ├── women.html
│   └── css/
└── backend/          
    ├── pom.xml
    └── src/main/java/com/karmicai/
        ├── KarmicAiApplication.java
        ├── SecurityConfig.java
        ├── controller/
        ├── model/
        ├── repository/
        └── service/

## How to access different view for different type of users

### Regular Doctors
1. Open `frontend/index.html`
2. Click "Get Started" → signup.html
3. Select a role (Intern / Junior Resident / Senior Resident / Consultant)
4. Create account → lands on user dashboard

### Admin view
1. On home page, click on Admin view in the top right corner
2. OR on login — toggle **"Log in as Hospital Admin"**
3. Redirects to admin dashboard (anonymised department-level dashboard)

### Women Support
1. Login as any user
2. On dashboard.html sidebar → toggle **"Women-Safe Mode"**
3. Banner appears with **"Open Women Support →"** button
4. Sidebar shows **" Women Support"** link → women.html

## Run Backend

cd backend
mvn spring-boot:run

Runs on http://localhost:8080
Frontend auto-calls /api/* endpoints. Works in demo mode without backend.
