<?php
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: POST, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type, Authorization');

require_once '../db_config/database.php';
require_once '../db_config/audit_helper.php'; // Asegúrate de tener este helper

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit;
}

$data = json_decode(file_get_contents("php://input"));

if (empty($data->name) || empty($data->apellido) || empty($data->email) || 
    empty($data->password) || empty($data->rol)) {
    echo json_encode(['success' => false, 'message' => 'Campos requeridos faltantes']);
    exit;
}

// 1. Validar Token (Para Auditoría)
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

    // Obtener creador
    $creador = obtenerUsuarioDesdeToken($db, $token);
    if (!$creador) {
        http_response_code(401);
        echo json_encode(["success" => false, "message" => "Token inválido"]);
        exit;
    }

    // Verificar email
    $checkQuery = "SELECT id FROM users WHERE email = :email AND eliminado = 0";
    $checkStmt = $db->prepare($checkQuery);
    $checkStmt->execute([':email' => $data->email]);
    if ($checkStmt->rowCount() > 0) {
        echo json_encode(['success' => false, 'message' => 'El email ya está registrado']);
        exit;
    }

    $db->beginTransaction();

    // Insertar usuario
    $query = "INSERT INTO users (name, apellido, email, password, telefono, rol, estado, created_at) 
              VALUES (:name, :apellido, :email, :password, :telefono, :rol, 'activo', NOW())";
    $stmt = $db->prepare($query);
    $hashedPassword = password_hash($data->password, PASSWORD_BCRYPT);
    
    $stmt->execute([
        ':name' => $data->name,
        ':apellido' => $data->apellido,
        ':email' => $data->email,
        ':password' => $hashedPassword,
        ':telefono' => $data->telefono,
        ':rol' => $data->rol
    ]);

    $newUserId = $db->lastInsertId();

    // 2. Construir Auditoría
    $device_info = [
        'modelo' => isset($data->device_model) ? $data->device_model : 'No disponible',
        'android' => isset($data->android_version) ? $data->android_version : 'No disponible',
        'ip' => obtenerIPCliente()
    ];

    $descripcion = "Creó nuevo usuario: {$data->name} {$data->apellido} ({$data->rol})\n";
    $descripcion .= "Info Dispositivo:\n- Modelo: {$device_info['modelo']}\n- Android: {$device_info['android']}\n- IP: {$device_info['ip']}";

    $queryAudit = "INSERT INTO auditoria_logs (user_id, accion, descripcion_detallada, tabla_afectada, id_registro_afectado, ip_address, fecha_accion, created_at) VALUES (:uid, :acc, :desc, 'users', :ref_id, :ip, NOW(), NOW())";
    $stmtAudit = $db->prepare($queryAudit);
    $stmtAudit->execute([
        ':uid' => $creador['id'],
        ':acc' => "Crear Usuario",
        ':desc' => $descripcion,
        ':ref_id' => $newUserId,
        ':ip' => $device_info['ip']
    ]);

    $db->commit();

    echo json_encode(['success' => true, 'message' => 'Usuario creado exitosamente']);

} catch (Exception $e) {
    $db->rollBack();
    echo json_encode(['success' => false, 'message' => 'Error: ' . $e->getMessage()]);
}
?>