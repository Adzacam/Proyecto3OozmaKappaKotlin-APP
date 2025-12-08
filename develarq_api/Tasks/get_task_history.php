<?php
// ============================================
// API: GET TASK HISTORY - Historial de cambios de una tarea
// ============================================
header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json; charset=UTF-8");
header("Access-Control-Allow-Methods: GET");
header("Access-Control-Allow-Headers: Content-Type, Access-Control-Allow-Headers, Authorization, X-Requested-With");

require_once '../db_config/database.php';
require_once '../db_config/audit_helper.php';

$database = new Database();
$db = $database->getConnection();

// ============================================
// VERIFICAR TOKEN
// ============================================
$headers = getallheaders();
$authHeader = isset($headers['Authorization']) ? $headers['Authorization'] : '';

if (empty($authHeader) || !preg_match('/Bearer\s(\S+)/', $authHeader, $matches)) {
    http_response_code(401);
    echo json_encode(["success" => false, "message" => "Token no proporcionado"]);
    exit();
}

$token = $matches[1];
$usuario = obtenerUsuarioDesdeToken($db, $token);

if (!$usuario) {
    http_response_code(401);
    echo json_encode(["success" => false, "message" => "Token inválido"]);
    exit();
}

// ============================================
// OBTENER PARÁMETROS
// ============================================
$tareaId = isset($_GET['tarea_id']) ? (int)$_GET['tarea_id'] : null;

if (!$tareaId) {
    http_response_code(400);
    echo json_encode(["success" => false, "message" => "ID de tarea requerido"]);
    exit();
}

try {
    // ============================================
    // VERIFICAR ACCESO A LA TAREA
    // ============================================
    $queryTarea = "SELECT t.proyecto_id 
                   FROM tareas t
                   INNER JOIN proyectos p ON t.proyecto_id = p.id
                   LEFT JOIN proyectos_usuarios pu ON p.id = pu.proyecto_id AND pu.user_id = :user_id
                   WHERE t.id = :tarea_id
                   AND (
                       p.responsable_id = :user_id
                       OR p.cliente_id = :user_id
                       OR pu.user_id = :user_id
                       OR :user_rol = 'admin'
                   )";
    
    $stmtTarea = $db->prepare($queryTarea);
    $stmtTarea->execute([
        ':tarea_id' => $tareaId,
        ':user_id' => $usuario['id'],
        ':user_rol' => $usuario['rol']
    ]);

    if ($stmtTarea->rowCount() === 0) {
        throw new Exception("No tienes acceso a esta tarea");
    }

    // ============================================
    // OBTENER HISTORIAL
    // ============================================
    $query = "SELECT 
                th.id,
                th.tarea_id,
                th.usuario_id,
                CONCAT(u.name, ' ', u.apellido) as usuario_nombre,
                th.estado_anterior,
                th.estado_nuevo,
                th.cambio,
                DATE_FORMAT(th.fecha_cambio, '%d/%m/%Y %H:%i') as fecha_cambio,
                th.created_at
              FROM tarea_historials th
              INNER JOIN users u ON th.usuario_id = u.id
              WHERE th.tarea_id = :tarea_id
              ORDER BY th.fecha_cambio DESC, th.id DESC";

    $stmt = $db->prepare($query);
    $stmt->execute([':tarea_id' => $tareaId]);

    $historial = $stmt->fetchAll(PDO::FETCH_ASSOC);

    // ============================================
    // RESPUESTA
    // ============================================
    http_response_code(200);
    echo json_encode([
        "success" => true,
        "message" => "Historial obtenido exitosamente",
        "data" => $historial
    ], JSON_UNESCAPED_UNICODE);

} catch (Exception $e) {
    http_response_code(500);
    echo json_encode([
        "success" => false,
        "message" => "Error al obtener historial: " . $e->getMessage()
    ]);
}
?>