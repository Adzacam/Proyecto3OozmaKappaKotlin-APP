<?php
header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json; charset=UTF-8");
header("Access-Control-Allow-Methods: GET");
header("Access-Control-Allow-Headers: Content-Type, Access-Control-Allow-Headers, Authorization, X-Requested-With");

require_once '../db_config/database.php';

$headers = apache_request_headers();
$token = isset($headers['Authorization']) ? str_replace('Bearer ', '', $headers['Authorization']) : null;

if (!$token) {
    http_response_code(401);
    echo json_encode(["success" => false, "message" => "Token no proporcionado"]);
    exit();
}

$database = new Database();
$db = $database->getConnection();

try {
    // Verificar token
    $query = "SELECT id FROM users WHERE remember_token = :token AND eliminado = 0";
    $stmt = $db->prepare($query);
    $stmt->bindParam(":token", $token);
    $stmt->execute();
    
    if ($stmt->rowCount() == 0) {
        http_response_code(401);
        echo json_encode(["success" => false, "message" => "Token inválido"]);
        exit();
    }
    
    // Obtener documentos eliminados
    $query = "SELECT 
                d.id,
                d.proyecto_id,
                d.nombre,
                d.descripcion,
                d.archivo_url,
                d.enlace_externo,
                d.tipo,
                d.fecha_subida,
                d.subido_por,
                d.eliminado,
                d.fecha_eliminacion,
                p.nombre as proyecto_nombre,
                CONCAT(u.name, ' ', u.apellido) as subido_por_nombre
              FROM documentos d
              INNER JOIN proyectos p ON d.proyecto_id = p.id
              LEFT JOIN users u ON d.subido_por = u.id
              WHERE d.eliminado = 1
              ORDER BY d.fecha_eliminacion DESC";
    
    $stmt = $db->prepare($query);
    $stmt->execute();
    $documents = $stmt->fetchAll(PDO::FETCH_ASSOC);
    
    http_response_code(200);
    echo json_encode([
        "success" => true,
        "data" => $documents,
        "message" => "Documentos eliminados obtenidos exitosamente"
    ]);
    
} catch (Exception $e) {
    http_response_code(500);
    echo json_encode([
        "success" => false,
        "message" => "Error: " . $e->getMessage()
    ]);
}
?>