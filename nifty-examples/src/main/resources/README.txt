The test certificate files were created with the openssl command line. They are self-signed certificates.
The procedure to re-create them (if it's ever needed) goes something like this:

openssl genrsa -out rsa.key 2048
openssl req -new -key rsa.key -out rsa.csr
# press [Enter] a bunch of times to accept default values for all fields
openssl x509 -req -days 10000 -in rsa.csr -signkey rsa.key -out rsa.crt
