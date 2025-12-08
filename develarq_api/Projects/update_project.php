<?php
// ... (Encabezados e includes igual que tu archivo original) ...
// ... (Autenticación igual) ...

$data = json_decode(file_get_contents("php://input"));
// ... (Recibir variables $id, $nombre, etc. igual que tu archivo) ...
$device_info = extraerInfoDispositivo($data);

// ... (Validaciones igual) ...

try {
    $db->beginTransaction();

    // 1. Actualizar Proyecto
    $query = "UPDATE proyectos SET nombre = :nombre, descripcion = :descripcion, estado = :estado, fecha_inicio = :fecha_inicio, fecha_fin = :fecha_fin, cliente_id = :cliente_id, responsable_id = :responsable_id, updated_at = NOW() WHERE id = :id";
    $stmt = $db->prepare($query);
    $stmt->execute([':nombre' => $nombre, ':descripcion' => $descripcion, ':estado' => $estado, ':fecha_inicio' => $fecha_inicio, ':fecha_fin' => $fecha_fin, ':cliente_id' => $cliente_id, ':responsable_id' => $responsable_id, ':id' => $id]);

    // 2. Actualizar Asignaciones (Tu lógica original de borrar e insertar es correcta)
    $query_delete = "DELETE FROM proyectos_usuarios WHERE proyecto_id = :proyecto_id AND rol_en_proyecto IN ('cliente', 'responsable')"; // OJO: Solo borrar roles fijos para no borrar invitados
    $stmt_delete = $db->prepare($query_delete);
    $stmt_delete->execute([':proyecto_id' => $id]);

    // Insertar Cliente y Responsable (Igual que tu archivo)...
    // [Asumiendo que mantienes tu código de inserción aquí]

    // 3. Auditoría
    registrarAuditoriaCompleta($db, $usuario['id'], "Actualizó el proyecto '$nombre'", 'proyectos', $id, $device_info);

    // 4. NOTIFICACIONES MASIVAS
    try {
        $query_usuarios = "SELECT DISTINCT user_id FROM proyectos_usuarios WHERE proyecto_id = :proyecto_id AND eliminado = 0";
        $stmt_usuarios = $db->prepare($query_usuarios);
        $stmt_usuarios->execute([':proyecto_id' => $id]);

        $query_notif = "INSERT INTO notificaciones (user_id, mensaje, tipo, asunto, url, created_at) VALUES (:user_id, :mensaje, 'proyecto', :asunto, :url, NOW())";
        $stmt_notif = $db->prepare($query_notif);

        while ($row = $stmt_usuarios->fetch(PDO::FETCH_ASSOC)) {
            $stmt_notif->execute([
                ':user_id' => $row['user_id'],
                ':mensaje' => "El proyecto '$nombre' ha sido actualizado.",
                ':asunto' => 'Actualización de Proyecto',
                ':url' => "http://127.0.0.1:8000/proyectos/$id"
            ]);
        }
    } catch (Exception $e) { error_log("Error notif: " . $e->getMessage()); }

    $db->commit();
    echo json_encode(["success" => true, "message" => "Proyecto actualizado exitosamente"]);

} catch (Exception $e) {
    $db->rollBack();
    http_response_code(500);
    echo json_encode(["success" => false, "message" => "Error: " . $e->getMessage()]);
}
?>