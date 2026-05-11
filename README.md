# CuidaVoz

CuidaVoz es una app web y móvil para seguimiento de medicación y presión arterial. Este repositorio contiene el backend en Next.js 16 con App Router, rutas API, Prisma y PostgreSQL.

## Desarrollo local

1. Instala dependencias:

```bash
npm install
```

2. Configura variables de entorno:

```bash
cp .env.example .env
```

3. Ejecuta migraciones y seed:

```bash
npx prisma migrate deploy
npm run db:seed
```

4. Inicia el backend:

```bash
npm run dev
```

## Scripts útiles

- `npm run dev`
- `npm run build`
- `npm run start`
- `npm run lint`
- `npm run db:seed`
- `npm run prisma:generate`
- `npm run prisma:migrate:deploy`

## Despliegue en Render con Supabase

### 1. Configurar Supabase

1. Crea un proyecto PostgreSQL en Supabase.
2. Copia la cadena de conexión en formato:

```env
DATABASE_URL="postgresql://USER:PASSWORD@HOST:PORT/DATABASE"
```

3. Usa esa URL como `DATABASE_URL` tanto en Render como en tareas locales de migración/seed de producción.

### 2. Configurar Render

1. Crea un `Web Service` apuntando a este repositorio.
2. Configura la variable de entorno requerida:

```env
DATABASE_URL
```

3. Usa estos comandos:

Build Command:

```bash
npm install && npx prisma generate && npm run build
```

Start Command:

```bash
npm run start
```

### 3. Migraciones y seed en producción

Ejecuta migraciones de producción con:

```bash
npx prisma migrate deploy
```

Si necesitas cargar el paciente demo base:

```bash
npm run db:seed
```

### 4. Notas operativas

- Prisma genera el cliente durante `postinstall` y durante `npm run build`.
- El cliente generado de Prisma debe permanecer ignorado por git.
- `POST /api/demo/reset` está deshabilitado en producción y responde `403`.
- Si `patient_maria` no existe, `GET /api/daily-status` responde `404` con:

```json
{
  "error": "Paciente demo no encontrado. Ejecuta npm run db:seed."
}
```

## Consumo desde móvil

La app Expo debe apuntar al backend desplegado configurando:

```env
EXPO_PUBLIC_API_URL=https://tu-backend-en-render.onrender.com
```
