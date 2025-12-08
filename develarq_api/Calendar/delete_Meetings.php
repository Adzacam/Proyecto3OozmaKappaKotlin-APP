<?php
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: POST, DELETE');
header('Access-Control-Allow-Headers: Content-Type, Authorization');

require_once '../db_config/database.php';
require_once '../db_config/audit_helper.php';

// Verificar autenticación
$headers = getallheaders();
if (!isset($headers['Authorization'])) {
    echo json_encode(['success' => false, 'message' => 'Token no proporcionado']);
    exit;
}

// Obtener ID de reunión desde la URL
$reunion_id = isset($_GET['id']) ? intval($_GET['id']) : 0;

if ($reunion_id <= 0) {
    echo json_encode(['success' => false, 'message' => 'ID de reunión inválido']);
    exit;
}

// Obtener datos del body
$data = json_decode(file_get_contents('php://input'), true);

try {
    $database = new Database();
    $pdo = $database->getConnection();
    
    // Obtener usuario desde token
    $token = str_replace('Bearer ', '', $headers['Authorization']);
    $usuario = obtenerUsuarioDesdeToken($pdo, $token);
    
    if (!$usuario) {
        http_response_code(401);
        echo json_encode(["success" => false, "message" => "Token inválido"]);
        exit;
    }
    
    $pdo->beginTransaction();
    
    // Verificar que la reunión existe
    $checkQuery = "
        SELECT r.id, r.titulo, r.proyecto_id, p.nombre as proyecto_nombre 
        FROM reuniones r
        LEFT JOIN proyectos p ON r.proyecto_id = p.id
        WHERE r.id = :id AND r.eliminado = 0
    ";
    $stmt = $pdo->prepare($checkQuery);
    $stmt->execute(['id' => $reunion_id]);
    $reunion = $stmt->fetch(PDO::FETCH_ASSOC);
    
    if (!$reunion) {
        $pdo->rollBack();
        echo json_encode(['success' => false, 'message' => 'Reunión no encontrada o ya eliminada']);
        exit;
    }

    // Obtener participantes antes de eliminar
    $participantesQuery = "
        SELECT user_id 
        FROM reuniones_usuarios 
        WHERE reunion_id = :reunion_id AND eliminado = 0
    ";
    $stmt = $pdo->prepare($participantesQuery);
    $stmt->execute(['reunion_id' => $reunion_id]);
    $participantes = $stmt->fetchAll(PDO::FETCH_COLUMN);
    
    // Marcar reunión como eliminada
    $deleteQuery = "UPDATE reuniones SET eliminado = 1, updated_at = NOW() WHERE id = :id";
    $stmt = $pdo->prepare($deleteQuery);
    $stmt->execute(['id' => $reunion_id]);
    
    // Marcar participantes como eliminados
    $deleteParticipantesQuery = "
        UPDATE reuniones_usuarios 
        SET eliminado = 1, updated_at = NOW() 
        WHERE reunion_id = :reunion_id
    ";
    $stmt = $pdo->prepare($deleteParticipantesQuery);
    $stmt->execute(['reunion_id' => $reunion_id]);

    // ✅ AUDITORÍA usando función centralizada
    $device_info = extraerInfoDispositivo($data);

    $descripcionAuditoria = "Eliminó reunión: '{$reunion['titulo']}'\n";
    $descripcionAuditoria .= "Proyecto: {$reunion['proyecto_nombre']}\n";
    $descripcionAuditoria .= "ID reunión: {$reunion_id}";

    registrarAuditoriaCompleta(
        $pdo,
        $usuario['id'],
        "Eliminó la reunión '{$reunion['titulo']}' del proyecto '{$reunion['proyecto_nombre']}'",
        'reuniones',
        $reunion_id,
        $device_info,
        $descripcionAuditoria
    );
    
    // Notificaciones
    if (!empty($participantes)) {
        $notificacionQuery = "
            INSERT INTO notificaciones (
                user_id, mensaje, tipo, asunto, url, leida, eliminado,
                fecha_envio, created_at, updated_at
            ) VALUES (
                :user_id, :mensaje, 'reunion', 'Reunión cancelada', 
                NULL, 0, 0, NOW(), NOW(), NOW()
            )
        ";
        
        $stmtNotificacion = $pdo->prepare($notificacionQuery);
        
        foreach ($participantes as $user_id) {
            $mensaje = "La reunión '{$reunion['titulo']}' del proyecto '{$reunion['proyecto_nombre']}' ha sido cancelada.";
            $stmtNotificacion->execute(['user_id' => $user_id, 'mensaje' => $mensaje]);
        }
    }
    
    $pdo->commit();
    
    echo json_encode([
        'success' => true,
        'message' => 'Reunión eliminada exitosamente'
    ]);
    
} catch (PDOException $e) {
    if (isset($pdo) && $pdo->inTransaction()) {
        $pdo->rollBack();
    }
    echo json_encode([
        'success' => false,
        'message' => 'Error al eliminar reunión: ' . $e->getMessage()
    ]);
}
?>