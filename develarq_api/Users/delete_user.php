<?php
// ==========================================
// delete_user.php (Soft delete)
// Ubicación: htdocs/develarq_api/Users/delete_user.php
// ==========================================
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: DELETE, POST, OPTIONS');
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
        $checkQuery = "SELECT COUNT(*) FROM projects 
                   WHERE responsable_id = :id AND eliminado = 0";
    
    $checkStmt = $db->prepare($checkQuery);
    $checkStmt->bindParam(':id', $data->id);
    $checkStmt->execute();
    $projectCount = $checkStmt->fetchColumn();
    
    if ($projectCount > 0) {
        // ¡Bloqueamos la eliminación!
        echo json_encode([
            'success' => false,
            'message' => 'No se puede eliminar: El usuario es responsable de ' . $projectCount . ' proyecto(s) activo(s).'
        ]);
        exit;
    }
    // Soft delete: marcar como eliminado
    $query = "UPDATE users 
              SET eliminado = 1, 
                  estado = 'inactivo',
                  updated_at = NOW()
              WHERE id = :id";
    
    $stmt = $db->prepare($query);
    $stmt->bindParam(':id', $data->id);
    
    if ($stmt->execute()) {
        echo json_encode([
            'success' => true,
            'message' => 'Usuario eliminado exitosamente'
        ]);
    } else {
        echo json_encode([
            'success' => false,
            'message' => 'Error al eliminar el usuario'
        ]);
    }

} catch (PDOException $e) {
    echo json_encode([
        'success' => false,
        'message' => 'Error: ' . $e->getMessage()
    ]);
}
?>