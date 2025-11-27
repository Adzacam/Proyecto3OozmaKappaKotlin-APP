<?php
// db_config/audit_helper.php

function registrarAuditoria($db, $user_id, $accion, $tabla_afectada, $id_registro_afectado = null, $ip_address = null) {
    try {
        // Si no se proporciona IP, intentar obtenerla
        if ($ip_address === null) {
            $ip_address = obtenerIPCliente();
        }
        
        $query = "INSERT INTO auditoria_logs 
                  (user_id, accion, tabla_afectada, id_registro_afectado, fecha_accion, ip_address, created_at) 
                  VALUES 
                  (:user_id, :accion, :tabla_afectada, :id_registro_afectado, NOW(), :ip_address, NOW())";
        
        $stmt = $db->prepare($query);
        $stmt->execute([
            ':user_id' => $user_id,
            ':accion' => $accion,
            ':tabla_afectada' => $tabla_afectada,
            ':id_registro_afectado' => $id_registro_afectado,
            ':ip_address' => $ip_address
        ]);
        
        return true;
    } catch (Exception $e) {
        // Log el error pero no interrumpir la operación principal
        error_log("Error al registrar auditoría: " . $e->getMessage());
        return false;
    }
}

function obtenerIPCliente() {
    $ip = 'DESCONOCIDA';
    
    if (!empty($_SERVER['HTTP_CLIENT_IP'])) {
        $ip = $_SERVER['HTTP_CLIENT_IP'];
    } elseif (!empty($_SERVER['HTTP_X_FORWARDED_FOR'])) {
        $ip = $_SERVER['HTTP_X_FORWARDED_FOR'];
    } elseif (!empty($_SERVER['REMOTE_ADDR'])) {
        $ip = $_SERVER['REMOTE_ADDR'];
    }
    
    return $ip;
}

function obtenerUsuarioDesdeToken($db, $token) {
    try {
        $query = "SELECT id, CONCAT(name, ' ', apellido) as nombre_completo 
                  FROM users 
                  WHERE remember_token = :token AND eliminado = 0 
                  LIMIT 1";
        $stmt = $db->prepare($query);
        $stmt->execute([':token' => $token]);
        
        if ($stmt->rowCount() > 0) {
            return $stmt->fetch(PDO::FETCH_ASSOC);
        }
    } catch (Exception $e) {
        error_log("Error al obtener usuario: " . $e->getMessage());
    }
    
    return null;
}
?>