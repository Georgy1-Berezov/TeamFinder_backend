# TeamFinder Backend

## ?? ’аҐЎ®ў ­Ёп
- Java 17+
- PostgreSQL 14+

## ?? “бв ­®ўЄ  PostgreSQL
1. ‘Є з ©вҐ б https://www.postgresql.org/download/windows/
2. “бв ­®ўЁвҐ, § Ї®¬­ЁвҐ Ї а®«м ¤«п Ї®«м§®ў вҐ«п postgres
3. ‘®§¤ ©вҐ Ў §г ¤ ­­ле:
   psql -U postgres -c "CREATE DATABASE teamfinder;"

## ?? Ќ бва®©Є 
ЋваҐ¤ ЄвЁаг©вҐ д ©« backend/src/main/resources/application.conf
“Є ¦ЁвҐ бў®© Ї а®«м ®в PostgreSQL ў Ї®«Ґ password.

## ?? ‡ ЇгбЄ
1. ЋвЄа®©вҐ CMD ў Ї ЇЄҐ TeamFinder-export
2. ‘®ЎҐаЁвҐ Їа®ҐЄв:
   gradlew clean build
3. ‡ ЇгбвЁвҐ бҐаўҐа:
   gradlew :backend:run

## ? Џа®ўҐаЄ 
ЋвЄа®©вҐ Ўа г§Ґа: http://localhost:8080/health
„®«¦Ґ­ ўҐа­гвмбп JSON: {"status":"ok"}

## ?? API „®Єг¬Ґ­в жЁп
‚бҐ н­¤Ї®Ё­вл ®ЇЁб ­л ў д ©«Ґ API.md
