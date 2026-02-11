#!/bin/sh
set -e

cat > /etc/seaweedfs/s3.json <<EOF
{
  "identities": [
    {
      "name": "admin",
      "credentials": [
        {
          "accessKey": "${AWS_ACCESS_KEY_ID}",
          "secretKey": "${AWS_SECRET_ACCESS_KEY}"
        }
      ],
      "actions": ["Admin", "Read", "List", "Tagging", "Write"]
    },
    {
      "name": "anonymous",
      "actions": ["Read"]
    }
  ]
}
EOF

exec weed server -s3 -s3.config=/etc/seaweedfs/s3.json -dir=/data -ip.bind=0.0.0.0
