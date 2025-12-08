<?php
// ============================================
// ESTABLECER UNA VERSIÓN COMO ACTUAL
// ============================================
header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json; charset=UTF-8");
header("Access-Control-Allow-Methods: POST");

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
// VALIDAR PERMISOS
// ============================================
$rol_permitido = in_array(strtolower($usuario['rol']), ['admin', 'ingeniero', 'arquitecto']);
if (!$rol_permitido) {
    http_response_code(403);
    echo json_encode(["success" => false, "message" => "No tienes permisos"]);
    exit();
}

// ============================================
// RECIBIR DATOS
// ============================================
$data = json_decode(file_get_contents("php://input"));
$version_id = isset($data->version_id) ? intval($data->version_id) : null;
$device_info = extraerInfoDispositivo($data);

if (!$version_id) {
    http_response_code(400);
    echo json_encode(["success" => false, "message" => "ID de versión no proporcionado"]);
    exit();
}

// ============================================
// OBTENER INFORMACIÓN DE LA VERSIÓN
// ============================================
try {
    $query_info = "SELECT nombre, version FROM planos_bim WHERE id = :id";
    $stmt_info = $db->prepare($query_info);
    $stmt_info->execute([':id' => $version_id]);
    $version = $stmt_info->fetch(PDO::FETCH_ASSOC);
    
    if (!$version) {
        http_response_code(404);
        echo json_encode(["success" => false, "message" => "Versión no encontrada"]);
        exit();
    }
    
    // ============================================
    // REGISTRAR AUDITORÍA
    // ============================================
    registrarAuditoriaCompleta(
        $db,
        $usuario['id'],
        "Estableció la versión {$version['version']} del plano '{$version['nombre']}' como actual",
        'planos_bim',
        $version_id,
        $device_info
    );
    
    echo json_encode([
        "success" => true,
        "message" => "Versión actualizada correctamente",
        "data" => [
            "version" => $version['version'],
            "nombre" => $version['nombre']
        ]
    ]);
    
} catch (Exception $e) {
    http_response_code(500);
    echo json_encode([
        "success" => false,
        "message" => "Error: " . $e->getMessage()
    ]);
}
?>