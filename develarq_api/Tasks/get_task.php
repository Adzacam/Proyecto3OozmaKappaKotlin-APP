<?php
// ============================================
// API: GET TASKS - Obtener tareas del Kanban
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

$userId = $usuario['id'];
$userRole = strtolower($usuario['rol']);

// ============================================
// OBTENER PARÁMETROS
// ============================================
$proyectoId = isset($_GET['proyecto_id']) ? (int)$_GET['proyecto_id'] : null;
$asignadoId = isset($_GET['asignado_id']) ? (int)$_GET['asignado_id'] : null;
$estado = isset($_GET['estado']) ? $_GET['estado'] : null;
$prioridad = isset($_GET['prioridad']) ? $_GET['prioridad'] : null;

try {
    // ============================================
    // CONSTRUIR QUERY BASE
    // ============================================
    $query = "SELECT 
                t.id,
                t.proyecto_id,
                p.nombre as proyecto_nombre,
                t.titulo,
                t.descripcion,
                t.estado,
                t.prioridad,
                t.fecha_limite,
                t.asignado_id,
                CONCAT(u_asignado.name, ' ', u_asignado.apellido) as asignado_nombre,
                t.creador_id,
                CONCAT(u_creador.name, ' ', u_creador.apellido) as creador_nombre,
                t.created_at,
                t.updated_at,
                (SELECT COUNT(*) FROM tarea_historials th WHERE th.tarea_id = t.id) as total_cambios
              FROM tareas t
              INNER JOIN proyectos p ON t.proyecto_id = p.id
              LEFT JOIN users u_asignado ON t.asignado_id = u_asignado.id
              LEFT JOIN users u_creador ON t.creador_id = u_creador.id
              WHERE t.eliminado = 0";

    // ============================================
    // APLICAR FILTROS SEGÚN ROL
    // ============================================
    if ($userRole !== 'admin') {
        // Solo ver tareas de proyectos donde el usuario está asignado
        $query .= " AND (
            p.responsable_id = :user_id 
            OR p.cliente_id = :user_id
            OR EXISTS (
                SELECT 1 FROM proyectos_usuarios pu 
                WHERE pu.proyecto_id = p.id 
                AND pu.user_id = :user_id
                AND pu.eliminado = 0
            )
        )";
    }

    // Filtro por proyecto
    if ($proyectoId) {
        $query .= " AND t.proyecto_id = :proyecto_id";
    }

    // Filtro por asignado
    if ($asignadoId) {
        $query .= " AND t.asignado_id = :asignado_id";
    }

    // Filtro por estado
    if ($estado) {
        $query .= " AND t.estado = :estado";
    }

    // Filtro por prioridad
    if ($prioridad) {
        $query .= " AND t.prioridad = :prioridad";
    }

    $query .= " ORDER BY 
                FIELD(t.prioridad, 'alta', 'media', 'baja'),
                t.fecha_limite ASC,
                t.created_at DESC";

    // ============================================
    // EJECUTAR QUERY
    // ============================================
    $stmt = $db->prepare($query);

    // Bind de parámetros
    if ($userRole !== 'admin') {
        $stmt->bindParam(':user_id', $userId, PDO::PARAM_INT);
    }
    if ($proyectoId) {
        $stmt->bindParam(':proyecto_id', $proyectoId, PDO::PARAM_INT);
    }
    if ($asignadoId) {
        $stmt->bindParam(':asignado_id', $asignadoId, PDO::PARAM_INT);
    }
    if ($estado) {
        $stmt->bindParam(':estado', $estado, PDO::PARAM_STR);
    }
    if ($prioridad) {
        $stmt->bindParam(':prioridad', $prioridad, PDO::PARAM_STR);
    }

    $stmt->execute();
    $tareas = $stmt->fetchAll(PDO::FETCH_ASSOC);

    // ============================================
    // FORMATEAR RESPUESTA
    // ============================================
    http_response_code(200);
    echo json_encode([
        "success" => true,
        "message" => "Tareas obtenidas exitosamente",
        "data" => $tareas
    ], JSON_UNESCAPED_UNICODE);

} catch (Exception $e) {
    http_response_code(500);
    echo json_encode([
        "success" => false,
        "message" => "Error al obtener tareas: " . $e->getMessage()
    ]);
}
?>