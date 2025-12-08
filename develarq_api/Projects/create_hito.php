<?php
// ... (Headers y Auth igual) ...

$data = json_decode(file_get_contents("php://input"));
// ... (Variables y device_info igual) ...

try {
    // Insertar Hito
    $query = "INSERT INTO hitos (proyecto_id, nombre, fecha_hito, descripcion, estado, encargado_id, created_at, updated_at) VALUES (:proyecto_id, :nombre, :fecha_hito, :descripcion, :estado, :encargado_id, NOW(), NOW())";
    $stmt = $db->prepare($query);
    $stmt->execute([':proyecto_id' => $proyecto_id, ':nombre' => $nombre, ':fecha_hito' => $fecha_hito, ':descripcion' => $descripcion, ':estado' => $estado, ':encargado_id' => $encargado_id]);
    
    $hito_id = $db->lastInsertId();

    // Auditoría
    registrarAuditoriaCompleta($db, $usuario['id'], "Creó el hito '$nombre'", 'hitos', $hito_id, $device_info);

    // NOTIFICACIONES MASIVAS
    try {
        // Obtener nombre del proyecto para el mensaje
        $stmt_proj = $db->prepare("SELECT nombre FROM proyectos WHERE id = ?");
        $stmt_proj->execute([$proyecto_id]);
        $nom_proy = $stmt_proj->fetchColumn();

        $query_usuarios = "SELECT DISTINCT user_id FROM proyectos_usuarios WHERE proyecto_id = :proyecto_id AND eliminado = 0";
        $stmt_usuarios = $db->prepare($query_usuarios);
        $stmt_usuarios->execute([':proyecto_id' => $proyecto_id]);

        $query_notif = "INSERT INTO notificaciones (user_id, mensaje, tipo, asunto, url, created_at) VALUES (:user_id, :mensaje, 'hito', :asunto, :url, NOW())";
        $stmt_notif = $db->prepare($query_notif);

        while ($row = $stmt_usuarios->fetch(PDO::FETCH_ASSOC)) {
            $msg = ($row['user_id'] == $encargado_id) 
                ? "Se te ha asignado el nuevo hito '$nombre' en '$nom_proy'." 
                : "Nuevo hito '$nombre' creado en el proyecto '$nom_proy'.";
            
            $stmt_notif->execute([
                ':user_id' => $row['user_id'],
                ':mensaje' => $msg,
                ':asunto' => 'Nuevo Hito',
                ':url' => "http://127.0.0.1:8000/proyectos/$proyecto_id"
            ]);
        }
    } catch (Exception $e) { error_log("Error notif: " . $e->getMessage()); }

    echo json_encode(["success" => true, "message" => "Hito creado exitosamente", "data" => ["id" => $hito_id]]);

} catch (Exception $e) {
    http_response_code(500);
    echo json_encode(["success" => false, "message" => "Error: " . $e->getMessage()]);
}
?>