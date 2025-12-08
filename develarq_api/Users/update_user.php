<?php
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: PUT, POST, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type, Authorization');

require_once '../db_config/database.php';
require_once '../db_config/audit_helper.php';

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') { http_response_code(200); exit; }

$data = json_decode(file_get_contents("php://input"));

if (empty($data->id) || empty($data->name) || empty($data->email) || empty($data->rol)) {
    echo json_encode(['success' => false, 'message' => 'Datos incompletos']);
    exit;
}

// Validar Token
$headers = getallheaders();
$authHeader = isset($headers['Authorization']) ? $headers['Authorization'] : '';
if (empty($authHeader) || !preg_match('/Bearer\s(\S+)/', $authHeader, $matches)) {
    http_response_code(401); echo json_encode(["success" => false, "message" => "Token faltante"]); exit;
}
$token = $matches[1];

// Validaciones de contraseña (resumido de tu código original)
if (isset($data->password) && !empty($data->password)) {
    $pwd = $data->password;
    if (strlen($pwd) < 8 || !preg_match('/[A-Z]/', $pwd) || !preg_match('/[0-9]/', $pwd)) {
        echo json_encode(['success' => false, 'message' => 'Contraseña no cumple requisitos de seguridad']); exit;
    }
}

try {
    $database = new Database();
    $db = $database->getConnection();

    // Obtener editor
    $editor = obtenerUsuarioDesdeToken($db, $token);
    if (!$editor) { http_response_code(401); echo json_encode(["success" => false, "message" => "Token inválido"]); exit; }

    // Obtener datos anteriores para auditoría
    $stmtOld = $db->prepare("SELECT name, apellido, rol FROM users WHERE id = :id");
    $stmtOld->execute([':id' => $data->id]);
    $oldData = $stmtOld->fetch(PDO::FETCH_ASSOC);

    // Verificar email duplicado
    $checkStmt = $db->prepare("SELECT id FROM users WHERE email = :email AND id != :id AND eliminado = 0");
    $checkStmt->execute([':email' => $data->email, ':id' => $data->id]);
    if ($checkStmt->rowCount() > 0) {
        echo json_encode(['success' => false, 'message' => 'Email en uso']); exit;
    }

    $db->beginTransaction();

    // Query Update
    $sql = "UPDATE users SET name=:name, apellido=:apellido, email=:email, telefono=:tel, rol=:rol, updated_at=NOW()";
    if (!empty($data->password)) {
        $sql .= ", password=:pwd";
    }
    $sql .= " WHERE id=:id";

    $stmt = $db->prepare($sql);
    $params = [
        ':name' => $data->name, ':apellido' => $data->apellido, ':email' => $data->email,
        ':tel' => $data->telefono, ':rol' => $data->rol, ':id' => $data->id
    ];
    if (!empty($data->password)) {
        $params[':pwd'] = password_hash($data->password, PASSWORD_BCRYPT);
    }
    $stmt->execute($params);

    // Auditoría
    $device_info = [
        'modelo' => $data->device_model ?? 'N/A',
        'android' => $data->android_version ?? 'N/A',
        'sdk' => $data->sdk_version ?? 'N/A',
        'ip' => obtenerIPCliente()
    ];
    
    
    $cambios = "Actualizó usuario ID {$data->id}.\n";
    if ($oldData['rol'] != $data->rol) $cambios .= "Rol: {$oldData['rol']} -> {$data->rol}\n";
    if (!empty($data->password)) $cambios .= "Cambió la contraseña.\n";
    $cambios .= "Info Dispositivo: Modelo {$device_info['modelo']}, Android {$device_info['android']}, IP {$device_info['ip']}";

    $queryAudit = "INSERT INTO auditoria_logs (user_id, accion, descripcion_detallada, tabla_afectada, id_registro_afectado, ip_address, fecha_accion, created_at) VALUES (:uid, :acc, :desc, 'users', :ref_id, :ip, NOW(), NOW())";
    $stmtAudit = $db->prepare($queryAudit);
    $stmtAudit->execute([
        ':uid' => $editor['id'], ':acc' => "Actualizar Usuario", ':desc' => $cambios,
        ':ref_id' => $data->id, ':ip' => $device_info['ip']
    ]);

    $db->commit();
    echo json_encode(['success' => true, 'message' => 'Usuario actualizado']);

} catch (Exception $e) {
    $db->rollBack();
    echo json_encode(['success' => false, 'message' => 'Error: ' . $e->getMessage()]);
}
?>