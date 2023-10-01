set -eu

bash build.sh
rm build/conf/application.conf
gpg --batch --passphrase $CONFIG_ENCRYPTION_KEY_PROD -o build/conf/application.conf build/conf/application.conf.prod.gpg
rm build/conf/META-INF/persistence.xml
gpg --batch --passphrase $CONFIG_ENCRYPTION_KEY_PROD -o build/conf/META-INF/persistence.xml build/conf/META-INF/persistence.xml.prod.gpg
rm build/conf/META-INF/persistence.xml.prod.gpg
rm build/conf/application.conf.prod.gpg
