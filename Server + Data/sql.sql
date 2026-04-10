SELECT u.username, b.amount, b.created_at
FROM bids b
JOIN users u ON b.user_id = u.id
WHERE b.product_id = 1
ORDER BY b.amount DESC;