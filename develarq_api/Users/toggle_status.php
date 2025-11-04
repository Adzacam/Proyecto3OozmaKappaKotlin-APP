<?php
// ==========================================
// toggle_status.php (Activar/Desactivar)
// Ubicación: htdocs/develarq_api/Users/toggle_status.php
// ==========================================
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: POST, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type, Authorization');

require_once '../db_config/database.php';

$data = json_decode(file_get_contents("php://input"));

if (empty($data->id)) {
    echo json_encode([
        'success' => false,
        'message' => 'ID de usuario requerido'
    ]);
    exit;
}

try {
    $database = new Database();
    $db = $database->getConnection();

    // Obtener estado actual
    $query = "SELECT estado FROM users WHERE id = :id AND eliminado = 0";
    $stmt = $db->prepare($query);
    $stmt->bindParam(':id', $data->id);
    $stmt->execute();
    
    $user = $stmt->fetch(PDO::FETCH_ASSOC);
    
    if (!$user) {
        echo json_encode([
            'success' => false,
            'message' => 'Usuario no encontrado'
        ]);
        exit;
    }

    // Cambiar estado
    $newStatus = ($user['estado'] === 'activo') ? 'inactivo' : 'activo';
    
    $updateQuery = "UPDATE users SET estado = :estado, updated_at = NOW() WHERE id = :id";
    $updateStmt = $db->prepare($updateQuery);
    $updateStmt->bindParam(':estado', $newStatus);
    $updateStmt->bindParam(':id', $data->id);
    
    if ($updateStmt->execute()) {
        echo json_encode([
            'success' => true,
            'message' => 'Estado actualizado exitosamente',
            'data' => ['estado' => $newStatus]
        ]);
    } else {
        echo json_encode([
            'success' => false,
            'message' => 'Error al actualizar el estado'
        ]);
    }

} catch (PDOException $e) {
    echo json_encode([
        'success' => false,
        'message' => 'Error: ' . $e->getMessage()
    ]);
}
?>