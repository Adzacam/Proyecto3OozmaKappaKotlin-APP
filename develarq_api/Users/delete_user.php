<?php
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: DELETE, POST, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type, Authorization');

require_once '../db_config/database.php';
require_once '../db_config/audit_helper.php';

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit;
}

$data = json_decode(file_get_contents("php://input"));

// Validaciones básicas
if (empty($data->id)) {
    echo json_encode(['success' => false, 'message' => 'ID de usuario requerido']);
    exit;
}

if (empty($data->motivo) || strlen(trim($data->motivo)) < 30) {
    echo json_encode([
        'success' => false,
        'message' => 'El motivo de eliminación debe tener al menos 30 caracteres'
    ]);
    exit;
}

// Validar token
$headers = getallheaders();
$authHeader = isset($headers['Authorization']) ? $headers['Authorization'] : '';

if (empty($authHeader) || !preg_match('/Bearer\s(\S+)/', $authHeader, $matches)) {
    http_response_code(401);
    echo json_encode(["success" => false, "message" => "Token no proporcionado"]);
    exit;
}

$token = $matches[1];

try {
    $database = new Database();
    $db = $database->getConnection();
    
    // Obtener usuario autenticado
    $usuario = obtenerUsuarioDesdeToken($db, $token);
    if (!$usuario) {
        http_response_code(401);
        echo json_encode(["success" => false, "message" => "Token inválido"]);
        exit;
    }
    
    // Obtener información del usuario a eliminar
    $queryUser = "SELECT CONCAT(name, ' ', apellido) as nombre_completo FROM users WHERE id = :id";
    $stmtUser = $db->prepare($queryUser);
    $stmtUser->execute([':id' => $data->id]);
    $userToDelete = $stmtUser->fetch(PDO::FETCH_ASSOC);
    
    if (!$userToDelete) {
        echo json_encode(['success' => false, 'message' => 'Usuario no encontrado']);
        exit;
    }
    
    // Verificar si tiene proyectos activos
    $checkQuery = "SELECT COUNT(*) FROM proyectos WHERE responsable_id = :id AND eliminado = 0";
    $checkStmt = $db->prepare($checkQuery);
    $checkStmt->execute([':id' => $data->id]);
    $projectCount = $checkStmt->fetchColumn();
    
    if ($projectCount > 0) {
        echo json_encode([
            'success' => false,
            'message' => 'No se puede eliminar: El usuario es responsable de ' . $projectCount . ' proyecto(s) activo(s).',
            'has_active_projects' => true
        ]);
        exit;
    }
    
    $db->beginTransaction();
    
    try {
        // Soft delete del usuario
        $query = "UPDATE users 
                  SET eliminado = 1, estado = 'inactivo', updated_at = NOW()
                  WHERE id = :id";
        
        $stmt = $db->prepare($query);
        $stmt->execute([':id' => $data->id]);
        
        // ✅ REGISTRAR AUDITORÍA USANDO LA FUNCIÓN CENTRALIZADA
        $device_info = extraerInfoDispositivo($data);
        
        registrarAuditoriaCompleta(
            $db,
            $usuario['id'],
            "Eliminó el usuario '{$userToDelete['nombre_completo']}'",
            'users',
            $data->id,
            $device_info,
            trim($data->motivo) // 👈 Motivo/justificación
        );
        
        $db->commit();
        
        echo json_encode([
            'success' => true,
            'message' => 'Usuario eliminado exitosamente'
        ]);
        
    } catch (Exception $e) {
        $db->rollBack();
        throw $e;
    }

} catch (PDOException $e) {
    echo json_encode([
        'success' => false,
        'message' => 'Error: ' . $e->getMessage()
    ]);
}
?>