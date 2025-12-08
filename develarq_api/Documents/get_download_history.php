<?php
header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json; charset=UTF-8");
header("Access-Control-Allow-Methods: GET, OPTIONS");
header("Access-Control-Allow-Headers: Content-Type, Authorization");

if ($_SERVER['REQUEST_METHOD'] == 'OPTIONS') {
    http_response_code(200);
    exit();
}

require_once '../db_config/database.php';

// Validación de token
$headers = getallheaders();
if (!isset($headers['Authorization']) && isset($_SERVER['HTTP_AUTHORIZATION'])) {
    $headers['Authorization'] = $_SERVER['HTTP_AUTHORIZATION'];
}

if (!isset($headers['Authorization'])) {
    http_response_code(401);
    echo json_encode(["success" => false, "message" => "Token no proporcionado"]);
    exit();
}

$database = new Database();
$db = $database->getConnection();

try {
    // Construir query con JOINs y alias claros
    $query = "SELECT 
                dh.id,
                dh.user_id,
                dh.documento_id,
                CONCAT(u.name, ' ', u.apellido) as user_name,
                d.nombre as documento_nombre,
                d.proyecto_id,
                DATE_FORMAT(dh.created_at, '%Y-%m-%d %H:%i:%s') as fecha_descarga,
                p.nombre as proyecto_nombre
              FROM descargas_historial dh
              INNER JOIN users u ON dh.user_id = u.id
              INNER JOIN documentos d ON dh.documento_id = d.id
              LEFT JOIN proyectos p ON d.proyecto_id = p.id
              WHERE d.eliminado = 0
              ORDER BY dh.created_at DESC";
    
    $stmt = $db->prepare($query);
    $stmt->execute();
    
    $downloads = [];
    
    while ($row = $stmt->fetch(PDO::FETCH_ASSOC)) {
        
        $downloads[] = [
            "id" => (int)$row['id'],
            // Campos para DownloadRecord (Android)
            "documento" => $row['documento_nombre'], 
            "usuario" => $row['user_name'],
            "fecha" => $row['fecha_descarga'],
            "proyecto" => $row['proyecto_nombre'],
            
            // Campos extra por si acaso se usan en filtros
            "userId" => (int)$row['user_id'],
            "documentoId" => (int)$row['documento_id'],
            "proyectoId" => $row['proyecto_id'] ? (int)$row['proyecto_id'] : null
        ];
    }
    
    http_response_code(200);
    echo json_encode([
        "success" => true,
        "message" => "Historial cargado correctamente",
        "data" => $downloads
    ]);
    
} catch (Exception $e) {
    http_response_code(500);
    echo json_encode([
        "success" => false,
        "message" => "Error: " . $e->getMessage()
    ]);
}
?>