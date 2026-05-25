# Oracle JDBC Dependency Resolution Issue

## Problem
Maven cannot resolve Oracle JDBC driver and security libraries:
- `com.oracle.database.security:osdt_core:23.3.0.23.09`
- `com.oracle.database.security:osdt_cert:23.3.0.23.09`
- `com.oracle.database.jdbc:ojdbc11:23.3.0.23.09`

**Root Cause**: Oracle JDBC drivers are not available in Maven Central Repository. They are only hosted on Oracle's private Maven repository which requires authentication.

---

## Solution: Choose One Option

### ✅ OPTION 1: Configure Oracle Repository Authentication (Recommended if you have Oracle account)

1. **Get Oracle credentials:**
   - Sign up or login at https://learn.oracle.com
   - Create/find your Oracle account username and password

2. **Update `~/.m2/settings.xml`:**
   ```xml
   <server>
       <id>oracle</id>
       <username>YOUR_ORACLE_USERNAME</username>
       <password>YOUR_ORACLE_PASSWORD</password>
   </server>
   ```

3. **Update project pom.xml repositories section:**
   ```xml
   <repositories>
       <repository>
           <id>oracle</id>
           <name>Oracle Repository</name>
           <url>https://maven.oracle.com</url>
           <releases>
               <enabled>true</enabled>
           </releases>
       </repository>
   </repositories>
   ```

4. **Run build:**
   ```bash
   mvn clean install
   ```

---

### ✅ OPTION 2: Download Jar Manually and Install Locally (No credentials needed)

1. **Download Oracle JDBC drivers:**
   - Visit https://www.oracle.com/database/technologies/appdev/jdbc-downloads.html
   - Download Oracle Database 23c JDBC Driver (`ojdbc11.jar`)
   - Also download these support JARs:
     - `oraclepki.jar`
     - `osdt_core.jar`
     - `osdt_cert.jar`

2. **Install each JAR to local Maven repository:**
   ```powershell
   # Example for ojdbc11
   mvn install:install-file `
     -Dfile=C:\path\to\ojdbc11.jar `
     -DgroupId=com.oracle.database.jdbc `
     -DartifactId=ojdbc11 `
     -Dversion=23.3.0.23.09 `
     -Dpackaging=jar

   # Install other JARs similarly with their respective artifact IDs
   ```

3. **Run build:**
   ```bash
   mvn clean install
   ```

---

### ✅ OPTION 3: Use Older JDBC Version Available in Maven Central

Edit `pom.xml` and use version `21.9.0.0` instead of `23.3.0.23.09`:

```xml
<properties>
    <oracle.jdbc.version>21.9.0.0</oracle.jdbc.version>
</properties>
```

This may have the security libraries available. Test:
```bash
mvn dependency:resolve
```

---

### ✅ OPTION 4: For Local Development Only - Use H2 in-memory DB

If Oracle database is only needed for integration tests:

1. **Make Oracle dependencies test-only:**
   ```xml
   <dependency>
       <groupId>com.oracle.database.jdbc</groupId>
       <artifactId>ojdbc11</artifactId>
       <version>${oracle.jdbc.version}</version>
       <scope>test</scope>
   </dependency>
   ```

2. **Use H2 for development:** Already configured in pom.xml

3. **Build should work now:**
   ```bash
   mvn clean install
   ```

---

### ✅ OPTION 5: Set up Corporate Repository Mirror (Artifact Repository)

If your organization uses Artifactory or Nexus:
1. Configure the mirror in `~/.m2/settings.xml`
2. Ensure the mirror has Oracle JDBC cached

---

## Recommended Next Steps

1. **Try Option 1** if you have an Oracle account
2. **Try Option 3** (older version) if Option 1 fails
3. **Try Option 2** (manual download) as fallback
4. If none of the above work, ask your team's Java/DevOps engineer for repository access

---

## Verify Fix

After applying one of the options above, run:
```bash
mvn clean install
```

If successful, you should see:
```
[INFO] BUILD SUCCESS
```

---

## Additional Resources

- [Oracle JDBC Drivers](https://www.oracle.com/database/technologies/appdev/jdbc-downloads.html)
- [Maven Settings Configuration](https://maven.apache.org/settings.html)
- [Maven Local Repository](https://maven.apache.org/repositories/index.html)
