param(
    [Parameter(Mandatory = $false)]
    [string] $ComposeService = "mariadb",

    [Parameter(Mandatory = $false)]
    [string] $Database = "discount_code",

    [Parameter(Mandatory = $false)]
    [string] $Username = "discount_code",

    [Parameter(Mandatory = $false)]
    [string] $Password = "discount_code"
)

$ErrorActionPreference = "Stop"

$sql = @"
START TRANSACTION;

DELETE usage_row
FROM coupon_usages usage_row
JOIN coupons coupon ON coupon.id = usage_row.coupon_id
WHERE coupon.code IN ('PLTEST', 'USTEST', 'LIMIT1');

INSERT INTO coupons (code, max_uses, country_code, current_uses)
VALUES
    ('PLTEST', 2, 'PL', 0),
    ('USTEST', 1, 'US', 0),
    ('LIMIT1', 1, 'US', 0)
ON DUPLICATE KEY UPDATE
    max_uses = VALUES(max_uses),
    country_code = VALUES(country_code),
    current_uses = 0;

COMMIT;

SELECT code, country_code, max_uses, current_uses
FROM coupons
WHERE code IN ('PLTEST', 'USTEST', 'LIMIT1')
ORDER BY code;
"@

Write-Host "Seeding demo coupons into Docker Compose service '$ComposeService'..."
$sql | docker compose exec -T $ComposeService mariadb "-u$Username" "-p$Password" $Database

Write-Host ""
Write-Host "Demo user IDs for redemption requests: user-1, user-2, user-3"
