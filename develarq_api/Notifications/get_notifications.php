<?php
// Notifications/get_notifications.php

header('Content-Type: application/json; charset=utf-8');
header("Access-Control-Allow-Origin: *");
header("Access-Control-Allow-Methods: GET, OPTIONS");
header("Access-Control-Allow-Headers: Content-Type, Authorization");

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit();
}

require_once '../db_config/database.php';

try {
    $database = new Database();
    $db = $database->getConnection();

    // Obtener token del header
    $headers = getallheaders();
    $token = isset($headers['Authorization']) ? str_replace('Bearer ', '', $headers['Authorization']) : null;

    if (!$token) {
        http_response_code(401);
        echo json_encode([
            "success" => false,
            "message" => "Token no proporcionado"
        ]);
        exit();
    }

    // Verificar token y obtener user_id
    $query = "SELECT id, rol FROM users WHERE remember_token = :token AND eliminado = 0 LIMIT 1";
    $stmt = $db->prepare($query);
    $stmt->execute([':token' => $token]);
    
    if ($stmt->rowCount() === 0) {
        http_response_code(401);
        echo json_encode([
            "success" => false,
            "message" => "Token inválido o usuario no encontrado"
        ]);
        exit();
    }

    $user = $stmt->fetch(PDO::FETCH_ASSOC);
    $user_id = $user['id'];
    $user_rol = $user['rol'];

    // Obtener parámetros opcionales
    $tipo = isset($_GET['tipo']) ? $_GET['tipo'] : null;
    $leida = isset($_GET['leida']) ? $_GET['leida'] : null;
    $limit = isset($_GET['limit']) ? (int)$_GET['limit'] : 50;

    // Construir query
    $sql = "SELECT 
                n.id,
                n.mensaje,
                n.tipo,
                n.asunto,
                n.url,
                n.leida,
                n.fecha_envio,
                n.created_at,
                CASE 
                    WHEN TIMESTAMPDIFF(HOUR, n.created_at, NOW()) < 24 THEN 1
                    ELSE 0
                END as es_nueva
            FROM notificaciones n
            WHERE n.user_id = :user_id 
            AND n.eliminado = 0";

    // Filtros opcionales
    if ($tipo) {
        $sql .= " AND n.tipo = :tipo";
    }
    
    if ($leida !== null) {
        $sql .= " AND n.leida = :leida";
    }

    $sql .= " ORDER BY n.fecha_envio DESC, n.id DESC LIMIT :limit";

    $stmt = $db->prepare($sql);
    $stmt->bindParam(':user_id', $user_id, PDO::PARAM_INT);
    
    if ($tipo) {
        $stmt->bindParam(':tipo', $tipo, PDO::PARAM_STR);
    }
    
    if ($leida !== null) {
        $stmt->bindParam(':leida', $leida, PDO::PARAM_INT);
    }
    
    $stmt->bindParam(':limit', $limit, PDO::PARAM_INT);
    $stmt->execute();

    $notificaciones = [];
    while ($row = $stmt->fetch(PDO::FETCH_ASSOC)) {
        $notificaciones[] = [
            'id' => (int)$row['id'],
            'mensaje' => $row['mensaje'],
            'tipo' => $row['tipo'],
            'asunto' => $row['asunto'],
            'url' => $row['url'],
            'leida' => (bool)$row['leida'],
            'es_nueva' => (bool)$row['es_nueva'],
            'fecha_envio' => $row['fecha_envio'],
            'created_at' => $row['created_at']
        ];
    }

    // Obtener contador de no leídas
    $queryCount = "SELECT COUNT(*) as total FROM notificaciones 
                   WHERE user_id = :user_id AND leida = 0 AND eliminado = 0";
    $stmtCount = $db->prepare($queryCount);
    $stmtCount->execute([':user_id' => $user_id]);
    $countResult = $stmtCount->fetch(PDO::FETCH_ASSOC);
    $no_leidas = (int)$countResult['total'];

    echo json_encode([
        "success" => true,
        "data" => $notificaciones,
        "no_leidas" => $no_leidas,
        "total" => count($notificaciones)
    ], JSON_UNESCAPED_UNICODE);

} catch (Exception $e) {
    http_response_code(500);
    echo json_encode([
        "success" => false,
        "message" => "Error al obtener notificaciones: " . $e->getMessage()
    ]);
}
?>