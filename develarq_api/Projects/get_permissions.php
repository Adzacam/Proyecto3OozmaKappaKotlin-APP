<?php
// ============================================
// OBTENER PERMISOS DEL PROYECTO
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

if (!$proyecto_id) {
    http_response_code(400);
    echo json_encode(["success" => false, "message" => "ID de proyecto requerido"]);
    exit();
}

// ============================================
// VERIFICAR PERMISOS DEL USUARIO
// ============================================
$rol_permitido = in_array(strtolower($usuario['rol']), ['admin', 'arquitecto']);
if (!$rol_permitido) {
    http_response_code(403);
    echo json_encode(["success" => false, "message" => "No tienes permisos para ver permisos del proyecto"]);
    exit();
}

// ============================================
// OBTENER PERMISOS
// ============================================
try {
    // Obtener todos los usuarios del sistema (excepto eliminados)
    $query = "SELECT 
              u.id as user_id,
              CONCAT(u.name, ' ', u.apellido) as user_nombre,
              u.rol,
              COALESCE(pu.permiso, 'ninguno') as permiso,
              pu.rol_en_proyecto
              FROM users u
              LEFT JOIN proyectos_usuarios pu ON u.id = pu.user_id AND pu.proyecto_id = :proyecto_id
              WHERE u.eliminado = 0
              ORDER BY u.name ASC";
    
    $stmt = $db->prepare($query);
    $stmt->execute([':proyecto_id' => $proyecto_id]);
    
    $permisos = [];
    while ($row = $stmt->fetch(PDO::FETCH_ASSOC)) {
        $permisos[] = [
            'user_id' => intval($row['user_id']),
            'user_nombre' => $row['user_nombre'],
            'permiso' => $row['permiso'],
            'rol_en_proyecto' => $row['rol_en_proyecto']
        ];
    }

    // ============================================
    // REGISTRAR AUDITORÍA (acceso)
    // ============================================
    $query_proyecto = "SELECT nombre FROM proyectos WHERE id = :proyecto_id";
    $stmt_proyecto = $db->prepare($query_proyecto);
    $stmt_proyecto->execute([':proyecto_id' => $proyecto_id]);
    $proyecto = $stmt_proyecto->fetch(PDO::FETCH_ASSOC);

    if ($proyecto) {
        registrarAuditoriaCompleta(
            $db,
            $usuario['id'],
            "Ingresó a gestión de permisos del proyecto '{$proyecto['nombre']}'.",
            'proyectos_usuarios',
            $proyecto_id,
            []
        );
    }
    
    echo json_encode([
        "success" => true,
        "data" => $permisos
    ]);
    
} catch (Exception $e) {
    http_response_code(500);
    echo json_encode([
        "success" => false,
        "message" => "Error al obtener permisos: " . $e->getMessage()
    ]);
}
?>