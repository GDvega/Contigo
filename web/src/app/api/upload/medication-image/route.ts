import { randomBytes } from "node:crypto";
import { mkdir, writeFile } from "node:fs/promises";
import path from "node:path";

import { NextResponse } from "next/server";

const MAX_FILE_SIZE = 5 * 1024 * 1024;
const UPLOAD_DIR = path.join(process.cwd(), "public", "uploads", "medications");

const allowedTypes = new Map([
  ["image/png", "png"],
  ["image/jpeg", "jpg"],
  ["image/webp", "webp"],
]);

const invalidImageMessage =
  "Solo puedes subir imágenes PNG, JPG o WEBP de hasta 5 MB.";

export async function POST(request: Request) {
  try {
    const formData = await request.formData();
    const file = formData.get("image");

    if (!(file instanceof File)) {
      return NextResponse.json({ message: invalidImageMessage }, { status: 400 });
    }

    const extension = allowedTypes.get(file.type);

    if (!extension || file.size > MAX_FILE_SIZE) {
      return NextResponse.json({ message: invalidImageMessage }, { status: 400 });
    }

    const bytes = await file.arrayBuffer();
    const buffer = Buffer.from(bytes);
    const filename = `medication-${Date.now()}-${randomBytes(6).toString(
      "hex"
    )}.${extension}`;

    await mkdir(UPLOAD_DIR, { recursive: true });
    await writeFile(path.join(UPLOAD_DIR, filename), buffer);

    return NextResponse.json({
      url: `/uploads/medications/${filename}`,
    });
  } catch {
    return NextResponse.json(
      { message: "No se pudo subir la imagen del medicamento." },
      { status: 500 }
    );
  }
}
