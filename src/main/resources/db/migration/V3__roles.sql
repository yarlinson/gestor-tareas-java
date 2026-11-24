-- Rol del usuario. Los usuarios existentes quedan como USER; los administradores se crean
-- al arrancar la aplicación a partir de las variables ADMIN_USERNAME y ADMIN_PASSWORD.
ALTER TABLE usuarios ADD COLUMN rol VARCHAR(10) NOT NULL DEFAULT 'USER';
ALTER TABLE usuarios ADD CONSTRAINT ck_usuarios_rol CHECK (rol IN ('USER', 'ADMIN'));
