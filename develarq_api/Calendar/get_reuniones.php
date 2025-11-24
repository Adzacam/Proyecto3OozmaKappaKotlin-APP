<?php
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, POST, PUT, DELETE');
header('Access-Control-Allow-Headers: Content-Type, Authorization');

require_once '../db_config/database.php';

// Verificar autenticación
$headers = getallheaders();
if (!isset($headers['Authorization'])) {
    echo json_encode(['success' => false, 'message' => 'Token no proporcionado']);
    exit;
}

try {
    // ✅ CORRECCIÓN: Instanciar la clase Database
    $database = new Database();
    $pdo = $database->getConnection();
    
    // Obtener todas las reuniones no eliminadas con información del proyecto
    $query = "
        SELECT 
            r.id,
            r.proyecto_id,
            p.nombre as proyecto_nombre,
            r.titulo,
            r.descripcion,
            r.fecha_hora,
            r.fecha_hora_fin,
            r.creador_id,
            r.eliminado
        FROM reuniones r
        LEFT JOIN proyectos p ON r.proyecto_id = p.id
        WHERE r.eliminado = 0
        ORDER BY r.fecha_hora DESC
    ";
    
    $stmt = $pdo->query($query);
    $meetings = $stmt->fetchAll(PDO::FETCH_ASSOC);
    
    // Obtener participantes para cada reunión
    foreach ($meetings as &$meeting) {
        $participantesQuery = "
            SELECT 
                ru.id,
                ru.user_id,
                CONCAT(u.name, ' ', u.apellido) as nombre,
                ru.asistio
            FROM reuniones_usuarios ru
            INNER JOIN users u ON ru.user_id = u.id
            WHERE ru.reunion_id = :reunion_id
            AND ru.eliminado = 0
        ";
        
        $stmtParticipantes = $pdo->prepare($participantesQuery);
        $stmtParticipantes->execute(['reunion_id' => $meeting['id']]);
        $meeting['participantes'] = $stmtParticipantes->fetchAll(PDO::FETCH_ASSOC);
    }
    
    echo json_encode([
        'success' => true,
        'data' => $meetings
    ]);
    
} catch (PDOException $e) {
    echo json_encode([
        'success' => false,
        'message' => 'Error en el servidor: ' . $e->getMessage()
    ]);
}
?>