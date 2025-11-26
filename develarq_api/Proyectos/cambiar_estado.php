<?php
// ==========================================
// cambiar_estado.php
// UBICACIÓN: htdocs/develarq_api/Proyectos/cambiar_estado.php
// ==========================================
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: POST, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type, Authorization');

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit;
}

require_once '../db_config/database.php';

$data = json_decode(file_get_contents("php://input"));

// Validar datos requeridos
if (!isset($_GET['id']) || empty($data->estado)) {
    echo json_encode([
        'success' => false,
        'message' => 'ID de proyecto y estado requeridos',
        'data' => null
    ]);
    exit;
}

$projectId = $_GET['id'];
$nuevoEstado = trim($data->estado);

// Validar que el estado sea válido
$estadosValidos = ['activo', 'en progreso', 'finalizado'];
if (!in_array($nuevoEstado, $estadosValidos)) {
    echo json_encode([
        'success' => false,
        'message' => 'Estado inválido. Debe ser: activo, en progreso o finalizado',
        'data' => null
    ]);
    exit;
}

try {
    $database = new Database();
    $db = $database->getConnection();

    // Verificar que el proyecto existe y no está eliminado
    $checkQuery = "SELECT id, nombre FROM proyectos WHERE id = :id AND eliminado = 0";
    $checkStmt = $db->prepare($checkQuery);
    $checkStmt->bindParam(':id', $projectId);
    $checkStmt->execute();

    if ($checkStmt->rowCount() === 0) {
        echo json_encode([
            'success' => false,
            'message' => 'Proyecto no encontrado',
            'data' => null
        ]);
        exit;
    }

    $proyecto = $checkStmt->fetch(PDO::FETCH_ASSOC);

    // Preparar query de actualización
    $query = "UPDATE proyectos SET estado = :estado";
    
    // Si el estado es "finalizado", guardar fecha de fin automáticamente
    if ($nuevoEstado === 'finalizado') {
        $query .= ", fecha_fin = NOW()";
    }
    
    $query .= ", updated_at = NOW() WHERE id = :id";
    
    $stmt = $db->prepare($query);
    $stmt->bindParam(':estado', $nuevoEstado);
    $stmt->bindParam(':id', $projectId);
    
    if ($stmt->execute()) {
        echo json_encode([
            'success' => true,
            'message' => 'Estado actualizado correctamente',
            'data' => [
                'id' => (int)$projectId,
                'nombre' => $proyecto['nombre'],
                'estado' => $nuevoEstado
            ]
        ]);
    } else {
        echo json_encode([
            'success' => false,
            'message' => 'Error al actualizar el estado',
            'data' => null
        ]);
    }

} catch (PDOException $e) {
    echo json_encode([
        'success' => false,
        'message' => 'Error de base de datos: ' . $e->getMessage(),
        'data' => null
    ]);
} catch (Exception $e) {
    echo json_encode([
        'success' => false,
        'message' => 'Error: ' . $e->getMessage(),
        'data' => null
    ]);
}
?>