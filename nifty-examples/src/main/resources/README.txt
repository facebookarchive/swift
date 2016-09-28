The test certificate files were created with the openssl command line. They are self-signed certificates.
The procedure to re-create them (if it's ever needed) goes something like this:

openssl genrsa -out rsa.key 2048
openssl req -new -key rsa.key -out rsa.csr
# press [Enter] a bunch of times to accept default values for all fields
openssl x509 -req -days 10000 -in rsa.csr -signkey rsa.key -out rsa.crt

To create the client.pkcs12 file:

cat client.key client.crt > client.pem
openssl pkcs12 -export -in client.pem -out client.pkcs12
# Type "12345" (without the quotes) twice for passphrase