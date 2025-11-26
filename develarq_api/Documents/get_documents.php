<?php
header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json; charset=UTF-8");
header("Access-Control-Allow-Methods: GET, OPTIONS");
header("Access-Control-Allow-Headers: Content-Type, Access-Control-Allow-Headers, Authorization, X-Requested-With");

if ($_SERVER['REQUEST_METHOD'] == 'OPTIONS') { http_response_code(200); exit(); }

require_once '../db_config/database.php';

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

    $proyecto_id = isset($_GET['proyecto_id']) ? intval($_GET['proyecto_id']) : null;
    
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
              WHERE d.eliminado = 0";
    
    if ($proyecto_id) {
        $query .= " AND d.proyecto_id = :proyecto_id";
    }
    
    $query .= " ORDER BY d.fecha_subida DESC";
    
    $stmt = $db->prepare($query);
    
    if ($proyecto_id) {
        $stmt->bindParam(":proyecto_id", $proyecto_id);
    }
    
    $stmt->execute();
    $documents = $stmt->fetchAll(PDO::FETCH_ASSOC);
    
    http_response_code(200);
    echo json_encode([
        "success" => true,
        "data" => $documents,
        "message" => "Documentos obtenidos exitosamente"
    ]);
    
} catch (Exception $e) {
    http_response_code(500);
    echo json_encode([
        "success" => false,
        "message" => "Error al obtener documentos: " . $e->getMessage()
    ]);
}
?>