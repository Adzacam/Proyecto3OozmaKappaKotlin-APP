<?php
// ============================================
// OBTENER HITOS DEL PROYECTO
// ============================================
header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json; charset=UTF-8");
header("Access-Control-Allow-Methods: GET");

include_once '../db_config/database.php';
include_once '../db_config/audit_helper.php';

$database = new Database();
$db = $database->getConnection();

// ============================================
// AUTENTICACIÓN
// ============================================
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

// ============================================
// OBTENER PARÁMETROS
// ============================================
$proyecto_id = isset($_GET['proyecto_id']) ? intval($_GET['proyecto_id']) : null;
$estado = isset($_GET['estado']) ? $_GET['estado'] : null;

if (!$proyecto_id) {
    http_response_code(400);
    echo json_encode(["success" => false, "message" => "ID de proyecto requerido"]);
    exit();
}

// ============================================
// CONSTRUIR QUERY
// ============================================
try {
    $query = "SELECT 
              h.id,
              h.proyecto_id,
              h.nombre,
              h.fecha_hito,
              h.descripcion,
              h.estado,
              h.encargado_id,
              CONCAT(u.name, ' ', u.apellido) as encargado_nombre,
              h.documento_id,
              h.created_at,
              h.updated_at
              FROM hitos h
              LEFT JOIN users u ON h.encargado_id = u.id
              WHERE h.proyecto_id = :proyecto_id";
    
    if ($estado) {
        $query .= " AND h.estado = :estado";
    }
    
    $query .= " ORDER BY h.fecha_hito ASC";
    
    $stmt = $db->prepare($query);
    
    if ($estado) {
        $stmt->execute([
            ':proyecto_id' => $proyecto_id,
            ':estado' => $estado
        ]);
    } else {
        $stmt->execute([':proyecto_id' => $proyecto_id]);
    }
    
    $hitos = [];
    while ($row = $stmt->fetch(PDO::FETCH_ASSOC)) {
        $hitos[] = [
            'id' => intval($row['id']),
            'proyecto_id' => intval($row['proyecto_id']),
            'nombre' => $row['nombre'],
            'fecha_hito' => $row['fecha_hito'],
            'descripcion' => $row['descripcion'],
            'estado' => $row['estado'],
            'encargado_id' => $row['encargado_id'] ? intval($row['encargado_id']) : null,
            'encargado_nombre' => $row['encargado_nombre'],
            'documento_id' => $row['documento_id'] ? intval($row['documento_id']) : null,
            'created_at' => $row['created_at'],
            'updated_at' => $row['updated_at']
        ];
    }
    
    echo json_encode([
        "success" => true,
        "data" => $hitos
    ]);
    
} catch (Exception $e) {
    http_response_code(500);
    echo json_encode([
        "success" => false,
        "message" => "Error al obtener hitos: " . $e->getMessage()
    ]);
}
?>