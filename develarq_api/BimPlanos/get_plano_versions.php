<?php
header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json; charset=UTF-8");

include_once '../db_config/database.php';
include_once '../db_config/audit_helper.php';

$database = new Database();
$db = $database->getConnection();

// AUTENTICACIÓN
$headers = getallheaders();
$token = isset($headers['Authorization']) ? str_replace('Bearer ', '', $headers['Authorization']) : null;

if (!$token) {
    http_response_code(401);
    echo json_encode(["success" => false, "message" => "Token no proporcionado"]);
    exit();
}

$usuario = obtenerUsuarioDesdeToken($db, $token);
if (!$usuario) {
    http_response_code(401);
    echo json_encode(["success" => false, "message" => "Token inválido"]);
    exit();
}

$plano_id = isset($_GET['plano_id']) ? intval($_GET['plano_id']) : null;

if (!$plano_id) {
    http_response_code(400);
    echo json_encode(["success" => false, "message" => "ID del plano no proporcionado"]);
    exit();
}

try {
    // Obtener el nombre y proyecto del plano solicitado
    $query_plano = "SELECT nombre, proyecto_id FROM planos_bim WHERE id = :plano_id AND eliminado = 0";
    $stmt_plano = $db->prepare($query_plano);
    $stmt_plano->execute([':plano_id' => $plano_id]);
    $plano_actual = $stmt_plano->fetch(PDO::FETCH_ASSOC);
    
    if (!$plano_actual) {
        http_response_code(404);
        echo json_encode(["success" => false, "message" => "Plano no encontrado"]);
        exit();
    }
    
    // Buscar TODAS las versiones con el mismo nombre en el mismo proyecto
    $query = "SELECT 
                pb.id,
                pb.nombre AS titulo,
                pb.version,
                pb.archivo_url,
                pb.tipo,
                pb.fecha_subida,
                pb.descripcion,
                CONCAT(u.name, ' ', u.apellido) AS subido_por_nombre,
                u.id AS subido_por_id,
                CASE 
                    WHEN pb.id = :plano_id THEN 1 
                    ELSE 0 
                END as es_version_actual
              FROM planos_bim pb
              LEFT JOIN users u ON pb.subido_por = u.id
              WHERE pb.nombre = :nombre 
              AND pb.proyecto_id = :proyecto_id
              AND pb.eliminado = 0
              ORDER BY 
                CAST(SUBSTRING_INDEX(COALESCE(pb.version, '1.0'), '.', 1) AS UNSIGNED) DESC,
                CAST(SUBSTRING_INDEX(COALESCE(pb.version, '1.0'), '.', -1) AS UNSIGNED) DESC,
                pb.fecha_subida DESC";
    
    $stmt = $db->prepare($query);
    $stmt->execute([
        ':plano_id' => $plano_id,
        ':nombre' => $plano_actual['nombre'],
        ':proyecto_id' => $plano_actual['proyecto_id']
    ]);
    
    $versiones = [];
    while ($row = $stmt->fetch(PDO::FETCH_ASSOC)) {
        $versiones[] = [
            'id' => intval($row['id']),
            'titulo' => $row['titulo'],
            'version' => $row['version'] ?? '1.0',
            'archivo_url' => $row['archivo_url'],
            'tipo' => $row['tipo'],
            'fecha_subida' => $row['fecha_subida'],
            'descripcion' => $row['descripcion'],
            'subido_por_nombre' => $row['subido_por_nombre'],
            'subido_por_id' => $row['subido_por_id'] ? intval($row['subido_por_id']) : null,
            'es_version_actual' => intval($row['es_version_actual']) == 1
        ];
    }
    
    echo json_encode([
        "success" => true,
        "data" => $versiones,
        "total_versiones" => count($versiones)
    ]);
    
} catch (Exception $e) {
    http_response_code(500);
    echo json_encode([
        "success" => false,
        "message" => "Error al obtener versiones: " . $e->getMessage()
    ]);
}
?>