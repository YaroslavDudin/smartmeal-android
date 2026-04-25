import os
from io import BytesIO
from PIL import Image
from django.core.files.base import ContentFile

def compress_image(image_field, max_width=1024, quality=75):
    """
    Compresses and resizes an image before saving.
    """
    if not image_field or not hasattr(image_field, 'path'):
        return

    try:
        # Open the image using Pillow
        img = Image.open(image_field)
        
        # Convert to RGB if necessary (e.g., for PNG/RGBA to JPEG)
        if img.mode in ("RGBA", "P"):
            img = img.convert("RGB")

        # Resize if width > max_width
        if img.width > max_width:
            new_height = int((max_width / img.width) * img.height)
            img = img.resize((max_width, new_height), Image.Resampling.LANCZOS)

        # Save to buffer
        buffer = BytesIO()
        img.save(buffer, format="JPEG", quality=quality, optimize=True)
        buffer.seek(0)

        # Get original filename but with .jpg extension
        name = os.path.basename(image_field.name)
        name = os.path.splitext(name)[0] + ".jpg"

        # Replace the image field content
        # We use save=False to avoid recursion since this is called from model.save()
        image_field.save(name, ContentFile(buffer.read()), save=False)
    except Exception as e:
        # If something goes wrong, just keep the original image
        print(f"Error compressing image: {e}")
