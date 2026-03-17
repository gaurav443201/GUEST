# Deployment Instructions for Guardian Health Backend on Render

1. **Prepare your GitHub Repository**:
   - Create a new GitHub repository.
   - Push the `backend/` folder into this new repository.

2. **Deploy on Render**:
   - Sign up or log in to [Render](https://render.com/).
   - Click on the **New +** button and select **Web Service**.
   - Connect your GitHub account and select your newly created repository.
   - Configure the following settings:
     - **Name**: `guardian-health-api` (or any unique name you prefer)
     - **Environment**: `Python 3`
     - **Root Directory**: Keep it blank if the contents of the `backend/` folder are at the root of your repo, otherwise type `backend`.
     - **Build Command**: `pip install -r requirements.txt`
     - **Start Command**: `uvicorn main:app --host 0.0.0.0 --port $PORT`
   - Click **Create Web Service**.

3. **Retrieve your Render URL**:
   - After successful deployment, Render will provide a public URL (e.g., `https://guardian-health-api.onrender.com`).

4. **Update the App & Web Dashboard**:
   - **Web Dashboard**: In `web_dashboard/index.html`, replace `http://localhost:8000` with the new Render URL.
   - **Android App**: In `android_app/app/src/main/java/com/example/guardianhealth/ApiService.kt`, replace `http://10.0.2.2:8000` with the correct Render URL.

Now your application will successfully communicate with the deployed real-time endpoint!
