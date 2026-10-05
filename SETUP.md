# NutriCheck — Transfer & Fresh Setup Guide (Windows)

This document provides complete, step-by-step instructions for moving and setting up the **NutriCheck Diet & Clinical Health Management System** on a fresh Windows laptop.

---

## 1. Prerequisites & Required Software

Before opening the project, ensure the following software is installed on the target machine:

### A. Java Development Kit (JDK 23)
- **Required Version**: **Java / JDK 23** (e.g. Oracle OpenJDK 23 or Eclipse Temurin 23).
- **Verification**: Open PowerShell / Command Prompt and verify:
  ```powershell
  java -version
  javac -version
  ```
  *(Ensure `JAVA_HOME` environment variable points to your JDK 23 installation directory).*

### B. IntelliJ IDEA
- **Recommended**: IntelliJ IDEA Community or Ultimate Edition.
- Ensure the **Maven** bundled plugin is enabled.

### C. MySQL Community Server 8.0+
- Install **MySQL Server 8.0+** (or MySQL Community installer).
- Default port: `3306`.
- Ensure the MySQL Windows service is running.

---

## 2. Preparing the Project for Transfer

### Folders & Files to EXCLUDE from Transfer
When copying the project to a USB drive or archive, **DO NOT copy the following generated/temporary folders**:

| Path / Pattern | Why Exclude |
| :--- | :--- |
| `target/` | Generated build binaries and compiler outputs (will be regenerated). |
| `.idea/`, `*.iml` | Machine-specific IDE metadata and workspace state. |
| `.env`, `.env.local` | Local secrets and environment files (must never be shared). |
| `.mvn/wrapper/maven-wrapper.jar` | *(Optional to exclude, but keeping `.mvn/` wrapper scripts is fine).* |

### Files that MUST be Transferred
- `src/` (All source code and HTML templates)
- `pom.xml` (Maven build and dependency definition)
- `mvnw`, `mvnw.cmd`, `.mvn/` (Maven wrapper for running builds without global Maven)
- `.env.example` (Configuration template)
- `README.md`, `SETUP.md`, `walkthrough.md`

---

## 3. Database Setup (MySQL)

### Option A: Fresh Database (Automatic Schema Generation)
NutriCheck uses Hibernate automatic schema management (`spring.jpa.hibernate.ddl-auto=update`).
You only need to ensure the database exists in MySQL:

1. Open **MySQL Command Line Client** or **MySQL Workbench**:
2. Run:
   ```sql
   CREATE DATABASE IF NOT EXISTS nutricheck_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   ```

### Option B: Transfer Existing Database Data (Full Data Backup & Restore)
If you want to transfer the exact existing records (patients, appointments, consultation logs, health history) from your current laptop:

1. **On Old Laptop (Export)**:
   ```powershell
   mysqldump -u root -p nutricheck_db > nutricheck_backup.sql
   ```
2. **Transfer `nutricheck_backup.sql`** to the new laptop.
3. **On New Laptop (Import)**:
   ```powershell
   mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS nutricheck_db;"
   mysql -u root -p nutricheck_db < nutricheck_backup.sql
   ```

---

## 4. Opening the Project in IntelliJ IDEA

1. Open IntelliJ IDEA.
2. Select **File > Open...** (or click **Open** from welcome screen).
3. Navigate to and select the project root folder (the folder containing `pom.xml`).
4. Click **OK** and choose **"Trust Project"**.
5. Configure Project SDK:
   - Go to **File > Project Structure... > Project Settings > Project**.
   - Set **SDK** to **JDK 23**.
   - Set **Language level** to **23 (or SDK default)**.
6. Allow IntelliJ to sync Maven dependencies (view progress in bottom status bar or open Maven tool window on the right and click **Reload All Maven Projects**).

---

## 5. Configuring Environment Variables in IntelliJ IDEA

NutriCheck separates secrets and credentials from source code. You must configure environment variables in your IntelliJ Run Configuration before starting the app.

> [!NOTE]
> `.env.example` in the project root is **only a template**. Standard Spring Boot does not automatically read `.env` files unless variables are configured in the IDE or system environment.

### Step-by-Step Configuration:

1. In IntelliJ IDEA, open `src/main/java/com/nutricheck/nutricheck/NutricheckApplication.java`.
2. Click the top toolbar Run Configuration dropdown (next to the green `▶` Run button) and select **Edit Configurations...** (or go to `Run > Edit Configurations...`).
3. Select **NutricheckApplication** under **Spring Boot** (or **Application**).
4. In the configuration window:
   - If the **Environment variables** field is not visible, click **Modify options** (blue link on right) and check **Environment variables**.
5. Click the folder/document icon on the right side of the **Environment variables** field and add the variables below (or paste them as semicolon-separated pairs):

```text
SPRING_DATASOURCE_PASSWORD=your_mysql_password;NUTRICHECK_ADMIN_PASSWORD=your_chosen_admin_password
```

### Environment Variables Reference Table:

| Variable Name | Required? | Default / Fallback | Purpose / Description |
| :--- | :---: | :--- | :--- |
| `SPRING_DATASOURCE_PASSWORD` | **REQUIRED** | *(Empty)* | Your local MySQL `root` user password. |
| `NUTRICHECK_ADMIN_PASSWORD` | **REQUIRED** *(for 1st run)* | *(Empty)* | Password for the initial administrator account. |
| `SPRING_DATASOURCE_URL` | Optional | `jdbc:mysql://localhost:3306/nutricheck_db...` | JDBC URL if running MySQL on a custom host/port. |
| `SPRING_DATASOURCE_USERNAME` | Optional | `root` | MySQL database username. |
| `NUTRICHECK_ADMIN_USERNAME` | Optional | `admin` | Username for the initial administrator. |
| `NUTRICHECK_ADMIN_EMAIL` | Optional | `admin@nutricheck.com` | Email for the initial administrator. |
| `NUTRICHECK_ADMIN_FULL_NAME` | Optional | `System Administrator` | Display name for the initial administrator. |
| `NUTRICHECK_REMEMBER_ME_KEY` | Optional | *(Ephemeral UUID)* | 256-bit string for persistent remember-me cookies across restarts. |

6. Click **Apply** and **OK**.

---

## 6. First-Administrator Provisioning Mechanism

When the application starts:
1. `DataInitializer.java` inspects the database to check if an admin account already exists.
2. If **no admin exists** and `NUTRICHECK_ADMIN_PASSWORD` is provided, it securely hashes the password with BCrypt and creates the root administrator (`admin`).
3. If `NUTRICHECK_ADMIN_PASSWORD` is **unset or blank**, the application safely skips admin creation and logs an explicit warning without using any silent default password.
4. On subsequent application startups, the initializer detects existing accounts and **never overwrites or resets administrator credentials**.

---

## 7. Running the Application

### Option A: From IntelliJ IDEA
- Click the green **Run (▶)** or **Debug** button in the top toolbar with `NutricheckApplication` selected.

### Option B: From Windows PowerShell Terminal
```powershell
# Set environment variables for the session
$env:SPRING_DATASOURCE_PASSWORD="your_mysql_password"
$env:NUTRICHECK_ADMIN_PASSWORD="your_chosen_admin_password"

# Run Spring Boot application
.\mvnw.cmd spring-boot:run
```

---

## 8. Accessing & Verifying the Application

Once started, open your browser and navigate to:
```
http://localhost:8080
```

### Role Verification Checklist:

| User Role | Username / Identifier | Password | Destination Dashboard | Accessible Features |
| :--- | :--- | :--- | :--- | :--- |
| **👑 Administrator** | `admin` (or configured email) | Value set in `NUTRICHECK_ADMIN_PASSWORD` | `http://localhost:8080/admin/dashboard` | Staff provisioning, user status management, system oversight, audit logs |
| **👤 New Patient** | Register at `/register` | User-defined password | `http://localhost:8080/patient/dashboard` | Public self-registration (strictly assigned `ROLE_PATIENT`) |

---

## 9. Running the Automated Test Suite

To verify system integrity and test execution on the new machine, run:

```powershell
.\mvnw.cmd test
```

### Expected Output:
- **`AdminProvisioningTest`**: 5 tests passed (Admin creation, password hashing, idempotency, Jackson JSON security, string protection).
- **`AuthAndSecurityTest`**: 9 tests passed (Role segregation, public registration protection, dashboard route guards).
- **`BmiServiceTest`**: 6 tests passed (WHO metric BMI category calculations and recommendations).
- **`NutricheckApplicationTests`**: 1 test passed (Spring context loading).
- **Total**: **21 tests, 0 failures, 0 errors**.
- **Result**: `BUILD SUCCESS`.

---

## 10. Common Troubleshooting

| Issue | Cause | Solution |
| :--- | :--- | :--- |
| `CommunicationsException: Communications link failure` | MySQL service is stopped or port 3306 is blocked | Start MySQL service via Windows Services (`services.msc`) or MySQL Notifier. |
| `Access denied for user 'root'@'localhost'` | Incorrect MySQL password | Ensure `SPRING_DATASOURCE_PASSWORD` matches your local MySQL root password. |
| `java: invalid source release: 23` | IntelliJ is using older JDK | Go to `File > Project Structure > Project` and set SDK and Language Level to 23. |
| Initial admin account not created | `NUTRICHECK_ADMIN_PASSWORD` was empty on 1st run | Set `NUTRICHECK_ADMIN_PASSWORD` in Run Configuration and restart the application. |
| Port 8080 already in use | Another application is running on port 8080 | Change `server.port=8081` in `src/main/resources/application.properties` or terminate the conflicting process. |
