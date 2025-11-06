<?php
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: DELETE');
header('Access-Control-Allow-Headers: Content-Type, Authorization');

require_once '../db_config/database.php';

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

try {
    $pdo = $database->getConnection();
    $pdo->beginTransaction();
    
    // Verificar que la reunión existe
    $checkQuery = "SELECT id, titulo FROM reuniones WHERE id = :id AND eliminado = 0";
    $stmt = $pdo->prepare($checkQuery);
    $stmt->execute(['id' => $reunion_id]);
    $reunion = $stmt->fetch(PDO::FETCH_ASSOC);
    
    if (!$reunion) {
        echo json_encode(['success' => false, 'message' => 'Reunión no encontrada']);
        exit;
    }
    
    // Marcar reunión como eliminada (soft delete)
    $deleteQuery = "UPDATE reuniones SET eliminado = 1 WHERE id = :id";
    $stmt = $pdo->prepare($deleteQuery);
    $stmt->execute(['id' => $reunion_id]);
    
    // También marcar participantes como eliminados
    $deleteParticipantesQuery = "UPDATE reuniones_usuarios SET eliminado = 1 WHERE reunion_id = :reunion_id";
    $stmt = $pdo->prepare($deleteParticipantesQuery);
    $stmt->execute(['reunion_id' => $reunion_id]);
    
    $pdo->commit();
    
    echo json_encode([
        'success' => true,
        'message' => 'Reunión eliminada exitosamente'
    ]);
    
} catch (PDOException $e) {
    $pdo->rollBack();
    echo json_encode([
        'success' => false,
        'message' => 'Error al eliminar reunión: ' . $e->getMessage()
    ]);
}
?>