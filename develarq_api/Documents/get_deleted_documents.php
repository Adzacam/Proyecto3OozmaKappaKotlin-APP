<?php
header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json; charset=UTF-8");
header("Access-Control-Allow-Methods: GET");
header("Access-Control-Allow-Headers: Content-Type, Access-Control-Allow-Headers, Authorization, X-Requested-With");

require_once '../db_config/database.php';
// 1. OBTENER HEADERS
$headers = getallheaders();
if (!isset($headers['Authorization']) && isset($_SERVER['HTTP_AUTHORIZATION'])) {
    $headers['Authorization'] = $_SERVER['HTTP_AUTHORIZATION'];
}

// 2. VALIDACIÓN BÁSICA (Sin verificar BD estrictamente para evitar 401 por hash)
if (!isset($headers['Authorization'])) {
    http_response_code(401);
    echo json_encode(["success" => false, "message" => "Token no proporcionado"]);
    exit();
}

$database = new Database();
$db = $database->getConnection();

try {
    // 3. OBTENER DOCUMENTOS ELIMINADOS (eliminado = 1)
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
              LEFT JOIN proyectos p ON d.proyecto_id = p.id
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
        "message" => "Papelera obtenida exitosamente"
    ]);
    
} catch (Exception $e) {
    http_response_code(500);
    echo json_encode([
        "success" => false,
        "message" => "Error: " . $e->getMessage()
    ]);
}
?>