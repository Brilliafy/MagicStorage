#!/usr/bin/env python3
import os, sys, json, zipfile, io
import numpy as np
from PIL import Image, ImageDraw, ImageFont

WIKI_DIR = "/home/Aelfwif/Downloads/magicstorageMinecraft/magicstorage/wiki"
RECIPES_DIR = os.path.join(WIKI_DIR, "recipes", "images")
os.makedirs(RECIPES_DIR, exist_ok=True)

MC_JAR = "/home/Aelfwif/.gradle/caches/minecraft/net/minecraft/minecraft/1.12.2/minecraft-1.12.2.jar"
RUSTIC_JAR = "/home/Aelfwif/Downloads/PrismLauncher-Linux-Qt6-Portable-11.0.3/instances/RLCraft Dregora/minecraft/mods/rustic-1.1.7.jar"
BB_JAR = "/home/Aelfwif/Downloads/PrismLauncher-Linux-Qt6-Portable-11.0.3/instances/RLCraft Dregora/minecraft/mods/Bountiful Baubles-1.12.2-0.1.8.jar"
QT_JAR = "/home/Aelfwif/Downloads/PrismLauncher-Linux-Qt6-Portable-11.0.3/instances/RLCraft Dregora/minecraft/mods/QualityTools-1.0.7_for_1.12.2.jar"
DIS_JAR = "/home/Aelfwif/Downloads/PrismLauncher-Linux-Qt6-Portable-11.0.3/instances/RLCraft Dregora/minecraft/mods/disenchanter[1.12]1.8.jar"
MOD_DIR = "/home/Aelfwif/Downloads/magicstorageMinecraft/magicstorage"

z_mc = zipfile.ZipFile(MC_JAR)
z_rustic = zipfile.ZipFile(RUSTIC_JAR)
z_bb = zipfile.ZipFile(BB_JAR)
z_qt = zipfile.ZipFile(QT_JAR)
z_dis = zipfile.ZipFile(DIS_JAR)

texture_cache = {}

BLOCK_ITEMS = {
    "storage_heart", "storage_access", "crafting_access", "remote_access", "hell_brick",
    "storage_unit_basic", "storage_unit_crimtane", "storage_unit_demonite", "storage_unit_hellstone",
    "storage_unit_hallowed", "storage_unit_blue_chlorophyte", "storage_unit_luminite", "storage_unit_terra",
    "diamond_block", "gold_block", "iron_block", "emerald_block", "lapis_block", "redstone_block",
    "obsidian", "sea_lantern", "crafting_table", "furnace", "enchanting_table", "anvil",
    "magma", "nether_brick_block", "nether_wart_block", "purpur_block", "ender_chest", "iron_ore",
    "log_wood", "chest_wood", "condenser_simple", "condenser_advanced", "retort_simple", "retort_advanced",
    "brewing_barrel", "crushing_tub", "disenchantment_table", "bb_reforger", "qt_reforging_station"
}

def get_texture_from_zip(z, path):
    try:
        data = z.read(path)
        im = Image.open(io.BytesIO(data)).convert("RGBA")
        if im.height > im.width:
            im = im.crop((0, 0, im.width, im.width))
        return im
    except Exception:
        return None

def get_texture_from_file(path):
    if os.path.exists(path):
        im = Image.open(path).convert("RGBA")
        if im.height > im.width:
            im = im.crop((0, 0, im.width, im.width))
        return im
    return None

def create_tinted_bottle(color_rgb):
    try:
        base = Image.open(io.BytesIO(z_rustic.read("assets/rustic/textures/items/elixir_bottle.png"))).convert("RGBA")
        overlay = Image.open(io.BytesIO(z_rustic.read("assets/rustic/textures/items/elixir_overlay.png"))).convert("RGBA")
        tinted = Image.new("RGBA", overlay.size, color_rgb + (255,))
        tinted.putalpha(overlay.getchannel("A"))
        return Image.alpha_composite(base, tinted)
    except Exception:
        return None

def find_coeffs(dst_pts, src_pts):
    matrix = []
    for p1, p2 in zip(dst_pts, src_pts):
        matrix.append([p1[0], p1[1], 1, 0, 0, 0, -p2[0]*p1[0], -p2[0]*p1[1]])
        matrix.append([0, 0, 0, p1[0], p1[1], 1, -p2[1]*p1[0], -p2[1]*p1[1]])
    A = np.matrix(matrix, dtype=float)
    B = np.array(src_pts).reshape(8)
    res = np.dot(np.linalg.inv(A.T * A) * A.T, B)
    return np.array(res).reshape(8)

def render_box(top_tex, left_tex, right_tex, from_xyz, to_xyz, size=64):
    out = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    scale = size / 32.0
    cx = size / 2.0
    cy = size / 2.0 + 8.0
    
    def project(x, y, z):
        dx = (x - 8.0)
        dy = (y - 8.0)
        dz = (z - 8.0)
        px = cx + (dx - dz) * 1.73205 * 0.5 * scale
        py = cy + (dx + dz) * 0.5 * 0.5 * scale - dy * scale
        return (int(round(px)), int(round(py)))
        
    x0, y0, z0 = from_xyz
    x1, y1, z1 = to_xyz
    
    p_top_L = project(x0, y1, z1)
    p_top_T = project(x0, y1, z0)
    p_top_R = project(x1, y1, z0)
    p_top_B = project(x1, y1, z1)
    
    p_left_TL = project(x0, y1, z1)
    p_left_TR = project(x1, y1, z1)
    p_left_BR = project(x1, y0, z1)
    p_left_BL = project(x0, y0, z1)
    
    p_right_TL = project(x1, y1, z1)
    p_right_TR = project(x1, y1, z0)
    p_right_BR = project(x1, y0, z0)
    p_right_BL = project(x1, y0, z1)
    
    src_pts = [(0, 0), (top_tex.width, 0), (top_tex.width, top_tex.height), (0, top_tex.height)]
    
    # Left face
    r, g, b, a = left_tex.split()
    r = r.point(lambda p: int(p * 0.82))
    g = g.point(lambda p: int(p * 0.82))
    b = b.point(lambda p: int(p * 0.82))
    sh_left = Image.merge("RGBA", (r, g, b, a))
    coeffs_left = find_coeffs([p_left_TL, p_left_TR, p_left_BR, p_left_BL], src_pts)
    warp_left = sh_left.transform((size, size), Image.PERSPECTIVE, coeffs_left, Image.NEAREST)
    out = Image.alpha_composite(out, warp_left)
    
    # Right face
    r, g, b, a = right_tex.split()
    r = r.point(lambda p: int(p * 0.62))
    g = g.point(lambda p: int(p * 0.62))
    b = b.point(lambda p: int(p * 0.62))
    sh_right = Image.merge("RGBA", (r, g, b, a))
    coeffs_right = find_coeffs([p_right_TL, p_right_TR, p_right_BR, p_right_BL], src_pts)
    warp_right = sh_right.transform((size, size), Image.PERSPECTIVE, coeffs_right, Image.NEAREST)
    out = Image.alpha_composite(out, warp_right)
    
    # Top face
    coeffs_top = find_coeffs([p_top_T, p_top_R, p_top_B, p_top_L], src_pts)
    warp_top = top_tex.transform((size, size), Image.PERSPECTIVE, coeffs_top, Image.NEAREST)
    out = Image.alpha_composite(out, warp_top)
    
    return out

def render_3d_block(top_tex, left_tex=None, right_tex=None, size=64):
    if left_tex is None: left_tex = top_tex
    if right_tex is None: right_tex = top_tex
    
    top_tex = top_tex.convert("RGBA").resize((16, 16), Image.NEAREST)
    left_tex = left_tex.convert("RGBA").resize((16, 16), Image.NEAREST)
    right_tex = right_tex.convert("RGBA").resize((16, 16), Image.NEAREST)
    
    src_pts = [(0, 0), (16, 0), (16, 16), (0, 16)]
    
    out = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    
    cx = size / 2.0
    s_w = size * 0.44
    s_h_top = size * 0.22
    s_h_side = size * 0.46
    
    top_v = (int(cx), int(size * 0.04))
    right_v = (int(cx + s_w), int(size * 0.04 + s_h_top))
    bot_v = (int(cx), int(size * 0.04 + 2 * s_h_top))
    left_v = (int(cx - s_w), int(size * 0.04 + s_h_top))
    
    bot_left_v = (left_v[0], left_v[1] + int(s_h_side))
    bot_center_v = (bot_v[0], bot_v[1] + int(s_h_side))
    bot_right_v = (right_v[0], right_v[1] + int(s_h_side))
    
    # 1. Left face (0.82x brightness)
    r, g, b, a = left_tex.split()
    r = r.point(lambda p: int(p * 0.82))
    g = g.point(lambda p: int(p * 0.82))
    b = b.point(lambda p: int(p * 0.82))
    shaded_left = Image.merge("RGBA", (r, g, b, a))
    dst_left = [left_v, bot_v, bot_center_v, bot_left_v]
    coeffs_left = find_coeffs(dst_left, src_pts)
    warp_left = shaded_left.transform((size, size), Image.PERSPECTIVE, coeffs_left, Image.NEAREST)
    out = Image.alpha_composite(out, warp_left)
    
    # 2. Right face (0.62x brightness)
    r, g, b, a = right_tex.split()
    r = r.point(lambda p: int(p * 0.62))
    g = g.point(lambda p: int(p * 0.62))
    b = b.point(lambda p: int(p * 0.62))
    shaded_right = Image.merge("RGBA", (r, g, b, a))
    dst_right = [bot_v, right_v, bot_right_v, bot_center_v]
    coeffs_right = find_coeffs(dst_right, src_pts)
    warp_right = shaded_right.transform((size, size), Image.PERSPECTIVE, coeffs_right, Image.NEAREST)
    out = Image.alpha_composite(out, warp_right)
    
    # 3. Top face (1.0x brightness)
    dst_top = [top_v, right_v, bot_v, left_v]
    coeffs_top = find_coeffs(dst_top, src_pts)
    warp_top = top_tex.transform((size, size), Image.PERSPECTIVE, coeffs_top, Image.NEAREST)
    out = Image.alpha_composite(out, warp_top)
    
    return out

def render_3d_beacon(size=64):
    glass = get_texture_from_zip(z_mc, "assets/minecraft/textures/blocks/glass.png")
    obsidian = get_texture_from_zip(z_mc, "assets/minecraft/textures/blocks/obsidian.png")
    beacon_core = get_texture_from_zip(z_mc, "assets/minecraft/textures/blocks/beacon.png")
    
    b_canvas = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    # 1. obsidian base [2, 0, 2] to [14, 3, 14]
    b_obs = render_box(obsidian, obsidian, obsidian, (2, 0, 2), (14, 3, 14), size)
    b_canvas = Image.alpha_composite(b_canvas, b_obs)
    # 2. beacon core [3, 3, 3] to [13, 14, 13]
    b_core = render_box(beacon_core, beacon_core, beacon_core, (3, 3, 3), (13, 14, 13), size)
    b_canvas = Image.alpha_composite(b_canvas, b_core)
    # 3. glass shell [0, 0, 0] to [16, 16, 16]
    b_glass = render_box(glass, glass, glass, (0, 0, 0), (16, 16, 16), size)
    b_canvas = Image.alpha_composite(b_canvas, b_glass)
    return b_canvas

def render_3d_end_rod(size=64):
    raw = get_texture_from_zip(z_mc, "assets/minecraft/textures/blocks/end_rod.png")
    base_side = raw.crop((2, 6, 6, 8)).resize((16, 16), Image.NEAREST)
    base_top = raw.crop((2, 0, 6, 4)).resize((16, 16), Image.NEAREST)
    shaft_tex = raw.crop((0, 0, 2, 14)).resize((16, 16), Image.NEAREST)
    
    rod_canvas = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    # 1. Base box: [6, 0, 6] to [10, 2, 10]
    rod_base = render_box(base_top, base_side, base_side, (6, 0, 6), (10, 2, 10), size)
    rod_canvas = Image.alpha_composite(rod_canvas, rod_base)
    # 2. Shaft box: [7, 2, 7] to [9, 16, 9]
    rod_shaft = render_box(shaft_tex, shaft_tex, shaft_tex, (7, 2, 7), (9, 16, 9), size)
    rod_canvas = Image.alpha_composite(rod_canvas, rod_shaft)
    return rod_canvas

def resolve_texture(name):
    if not name:
        return None
    if name in texture_cache:
        return texture_cache[name]
    
    # 1. Custom tinted composites
    if name == "wine":
        im = create_tinted_bottle((114, 24, 44))
        if im:
            texture_cache[name] = im
            return im
    if name == "grape_juice":
        im = create_tinted_bottle((88, 29, 99))
        if im:
            texture_cache[name] = im
            return im
    if name == "elixir":
        im = create_tinted_bottle((229, 168, 35))
        if im:
            texture_cache[name] = im
            return im
    
    # 2. Magic Storage assets
    ms_items = [
        os.path.join(MOD_DIR, "src/main/resources/assets/magicstorage/textures/item", name + ".png"),
        os.path.join(MOD_DIR, "src/main/resources/assets/magicstorage/textures/item", name.replace("_", "") + ".png")
    ]
    ms_blocks = [
        os.path.join(MOD_DIR, "src/main/resources/assets/magicstorage/textures/block", name + ".png")
    ]
    for p in ms_items + ms_blocks:
        im = get_texture_from_file(p)
        if im:
            texture_cache[name] = im
            return im
    
    # 3. Vanilla mappings
    vanilla_mappings = {
        "iron_ingot": "assets/minecraft/textures/items/iron_ingot.png",
        "gold_ingot": "assets/minecraft/textures/items/gold_ingot.png",
        "diamond": "assets/minecraft/textures/items/diamond.png",
        "emerald": "assets/minecraft/textures/items/emerald.png",
        "redstone": "assets/minecraft/textures/items/redstone_dust.png",
        "clock": "assets/minecraft/textures/items/clock_00.png",
        "compass": "assets/minecraft/textures/items/compass_00.png",
        "ender_pearl": "assets/minecraft/textures/items/ender_pearl.png",
        "ender_eye": "assets/minecraft/textures/items/ender_eye.png",
        "nether_star": "assets/minecraft/textures/items/nether_star.png",
        "shulker_shell": "assets/minecraft/textures/items/shulker_shell.png",
        "chorus_fruit_popped": "assets/minecraft/textures/items/chorus_fruit_popped.png",
        "dragon_breath": "assets/minecraft/textures/items/dragon_breath.png",
        "end_crystal": "assets/minecraft/textures/items/end_crystal.png",
        "blaze_powder": "assets/minecraft/textures/items/blaze_powder.png",
        "quartz": "assets/minecraft/textures/items/quartz.png",
        "stick": "assets/minecraft/textures/items/stick.png",
        "glass_bottle": "assets/minecraft/textures/items/potion_bottle_empty.png",
        "potion": "assets/minecraft/textures/items/potion_bottle_drinkable.png",
        "awkward_potion": "assets/minecraft/textures/items/potion_bottle_drinkable.png",
        "book": "assets/minecraft/textures/items/book_normal.png",
        "enchanted_book": "assets/minecraft/textures/items/book_enchanted.png",
        "diamond_sword": "assets/minecraft/textures/items/diamond_sword.png",
        "iron_sword": "assets/minecraft/textures/items/iron_sword.png",
        "diamond_pickaxe": "assets/minecraft/textures/items/diamond_pickaxe.png",
        "iron_pickaxe": "assets/minecraft/textures/items/iron_pickaxe.png",
        "nether_wart": "assets/minecraft/textures/items/nether_wart.png",
        "sugar": "assets/minecraft/textures/items/sugar.png",
        "wheat": "assets/minecraft/textures/items/wheat.png",
        "coal": "assets/minecraft/textures/items/coal.png",
        "charcoal": "assets/minecraft/textures/items/charcoal.png",
        "water_bucket": "assets/minecraft/textures/items/bucket_water.png",
        "bucket": "assets/minecraft/textures/items/bucket_empty.png",
        "leather": "assets/minecraft/textures/items/leather.png",
        "end_rod": "assets/minecraft/textures/blocks/end_rod.png",
        "lapis_block": "assets/minecraft/textures/blocks/lapis_block.png",
        "lapis_lazuli": "assets/minecraft/textures/items/dye_powder_blue.png",
        "diamond_block": "assets/minecraft/textures/blocks/diamond_block.png",
        "gold_block": "assets/minecraft/textures/blocks/gold_block.png",
        "iron_block": "assets/minecraft/textures/blocks/iron_block.png",
        "emerald_block": "assets/minecraft/textures/blocks/emerald_block.png",
        "redstone_block": "assets/minecraft/textures/blocks/redstone_block.png",
        "obsidian": "assets/minecraft/textures/blocks/obsidian.png",
        "sea_lantern": "assets/minecraft/textures/blocks/sea_lantern.png",
        "crafting_table": "assets/minecraft/textures/blocks/crafting_table_front.png",
        "furnace": "assets/minecraft/textures/blocks/furnace_front_off.png",
        "enchanting_table": "assets/minecraft/textures/blocks/enchanting_table_top.png",
        "anvil": "assets/minecraft/textures/blocks/anvil_top_damaged_0.png",
        "brewing_stand": "assets/minecraft/textures/items/brewing_stand.png",
        "beacon": "assets/minecraft/textures/blocks/beacon.png",
        "magma": "assets/minecraft/textures/blocks/magma.png",
        "netherbrick": "assets/minecraft/textures/items/netherbrick.png",
        "nether_brick_block": "assets/minecraft/textures/blocks/nether_brick.png",
        "nether_wart_block": "assets/minecraft/textures/blocks/nether_wart_block.png",
        "purpur_block": "assets/minecraft/textures/blocks/purpur_block.png",
        "ender_chest": "assets/minecraft/textures/blocks/obsidian.png",
        "iron_ore": "assets/minecraft/textures/blocks/iron_ore.png",
        "log_wood": "assets/minecraft/textures/blocks/log_oak.png",
        "chest_wood": "assets/minecraft/textures/blocks/planks_oak.png"
    }
    if name in vanilla_mappings:
        im = get_texture_from_zip(z_mc, vanilla_mappings[name])
        if im:
            texture_cache[name] = im
            return im
            
    # 4. Mod mappings
    rustic_mappings = {
        "grapes": "assets/rustic/textures/items/grapes.png",
        "wildberries": "assets/rustic/textures/items/wildberries.png",
        "ironberries": "assets/rustic/textures/items/ironberries.png",
        "chamomile": "assets/rustic/textures/items/herbs/chamomile.png",
        "aloe_vera": "assets/rustic/textures/items/herbs/aloe_vera.png",
        "blood_orchid": "assets/rustic/textures/items/herbs/blood_orchid.png",
        "cohosh": "assets/rustic/textures/items/herbs/cohosh.png",
        "horsetail": "assets/rustic/textures/items/herbs/horsetail.png",
        "condenser_simple": "assets/rustic/textures/blocks/condenser_simple_side.png",
        "condenser_advanced": "assets/rustic/textures/blocks/condenser_advanced_side.png",
        "retort_simple": "assets/rustic/textures/blocks/retort_simple.png",
        "retort_advanced": "assets/rustic/textures/blocks/retort_advanced.png",
        "brewing_barrel": "assets/rustic/textures/blocks/brewing_barrel_top.png",
        "crushing_tub": "assets/rustic/textures/blocks/crushing_tub_side.png"
    }
    if name in rustic_mappings:
        im = get_texture_from_zip(z_rustic, rustic_mappings[name])
        if im:
            texture_cache[name] = im
            return im

    bb_mappings = {
        "ring_overclocking": "assets/bountifulbaubles/textures/items/ring_overclocking.png",
        "shield_cobalt": "assets/bountifulbaubles/textures/items/shield_cobalt.png",
        "bezoar": "assets/bountifulbaubles/textures/items/trinket_bezoar.png",
        "spectral_silt": "assets/bountifulbaubles/textures/items/spectral_silt.png",
        "bb_reforger": "assets/bountifulbaubles/textures/blocks/reforger/workbench.png"
    }
    if name in bb_mappings:
        im = get_texture_from_zip(z_bb, bb_mappings[name])
        if im:
            texture_cache[name] = im
            return im

    qt_mappings = {
        "qt_reforging_station": "assets/qualitytools/textures/items/emerald_ring.png",
        "emerald_ring": "assets/qualitytools/textures/items/emerald_ring.png",
        "emerald_amulet": "assets/qualitytools/textures/items/emerald_amulet.png"
    }
    if name in qt_mappings:
        im = get_texture_from_zip(z_qt, qt_mappings[name])
        if im:
            texture_cache[name] = im
            return im

    dis_mappings = {
        "disenchantment_table": "assets/disenchanter/textures/blocks/disenchantmenttable_top.png"
    }
    if name in dis_mappings:
        im = get_texture_from_zip(z_dis, dis_mappings[name])
        if im:
            texture_cache[name] = im
            return im

    for f in z_mc.namelist():
        if f.startswith("assets/minecraft/textures/") and f.endswith(name + ".png"):
            im = get_texture_from_zip(z_mc, f)
            if im:
                texture_cache[name] = im
                return im

    print(f"Warning: Texture '{name}' not found.")
    return None

def get_slot_image(tex_name, target_size=64):
    if tex_name == "beacon":
        return render_3d_beacon(size=target_size)
    if tex_name == "end_rod":
        return render_3d_end_rod(size=target_size)
        
    tex = resolve_texture(tex_name)
    if not tex:
        return None
    if tex_name in BLOCK_ITEMS:
        return render_3d_block(tex, size=target_size)
    else:
        return tex.resize((target_size, target_size), Image.NEAREST)

def draw_beveled_box(draw, x0, y0, x1, y1, fill=(198, 198, 198), border_dark=(55, 55, 55), border_light=(255, 255, 255), bw=4):
    draw.rectangle([x0, y0, x1, y1], fill=fill)
    for i in range(bw):
        draw.line([x0 + i, y0 + i, x1 - i, y0 + i], fill=border_light)
        draw.line([x0 + i, y0 + i, x0 + i, y1 - i], fill=border_light)
    for i in range(bw):
        draw.line([x0 + i, y1 - i, x1 - i, y1 - i], fill=border_dark)
        draw.line([x1 - i, y0 + i, x1 - i, y1 - i], fill=border_dark)

def draw_slot(draw, x, y, size=72, bw=4):
    draw.rectangle([x, y, x + size, y + size], fill=(139, 139, 139))
    for i in range(bw):
        draw.line([x + i, y + i, x + size - i, y + i], fill=(55, 55, 55))
        draw.line([x + i, y + i, x + i, y + size - i], fill=(55, 55, 55))
        draw.line([x + i, y + size - i, x + size - i, y + size - i], fill=(255, 255, 255))
        draw.line([x + size - i, y + i, x + size - i, y + size - i], fill=(255, 255, 255))
    draw.rectangle([x + bw, y + bw, x + size - bw, y + size - bw], fill=(139, 139, 139))

def draw_arrow(draw, x, y, width=70, height=44):
    draw.rectangle([x, y + 14, x + 40, y + 30], fill=(139, 139, 139), outline=(55, 55, 55), width=3)
    draw.polygon([(x + 40, y), (x + width, y + height//2), (x + 40, y + height)], fill=(139, 139, 139), outline=(55, 55, 55))

def render_recipe_image(title, subtitle, grid_items, output_item, output_count=1, filename="recipe.png"):
    width = 820
    height = 420
    
    img = Image.new("RGBA", (width, height), (198, 198, 198, 255))
    draw = ImageDraw.Draw(img)
    
    draw_beveled_box(draw, 0, 0, width - 1, height - 1, fill=(198, 198, 198), bw=6)
    draw_beveled_box(draw, 16, 16, width - 17, 76, fill=(215, 215, 215), bw=3)
    
    try:
        font_title = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf", 22)
        font_sub = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf", 14)
        font_count = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf", 18)
    except Exception:
        font_title = ImageFont.load_default()
        font_sub = font_title
        font_count = font_title
        
    draw.text((28, 24), title, fill=(30, 30, 30), font=font_title)
    draw.text((28, 52), subtitle, fill=(80, 80, 80), font=font_sub)
    
    grid_start_x = 50
    grid_start_y = 105
    slot_size = 80
    slot_gap = 8
    
    for row in range(3):
        for col in range(3):
            sx = grid_start_x + col * (slot_size + slot_gap)
            sy = grid_start_y + row * (slot_size + slot_gap)
            draw_slot(draw, sx, sy, size=slot_size, bw=4)
            
            idx = row * 3 + col
            slot_info = grid_items[idx] if idx < len(grid_items) else None
            if slot_info:
                tex_name, count = (slot_info if isinstance(slot_info, tuple) else (slot_info, 1))
                slot_img = get_slot_image(tex_name, target_size=64)
                if slot_img:
                    img.paste(slot_img, (sx + 8, sy + 8), slot_img)
                    
                if count > 1:
                    c_str = str(count)
                    draw.text((sx + slot_size - 22, sy + slot_size - 24), c_str, fill=(40, 40, 40), font=font_count)
                    draw.text((sx + slot_size - 24, sy + slot_size - 26), c_str, fill=(255, 255, 255), font=font_count)
    
    arrow_x = grid_start_x + 3 * (slot_size + slot_gap) + 30
    arrow_y = grid_start_y + (3 * slot_size + 2 * slot_gap) // 2 - 22
    draw_arrow(draw, arrow_x, arrow_y, width=80, height=44)
    
    out_size = 104
    out_x = arrow_x + 115
    out_y = grid_start_y + (3 * slot_size + 2 * slot_gap) // 2 - out_size // 2
    draw_slot(draw, out_x, out_y, size=out_size, bw=6)
    
    out_tex_name, out_cnt = (output_item if isinstance(output_item, tuple) else (output_item, output_count))
    out_img = get_slot_image(out_tex_name, target_size=84)
    if out_img:
        img.paste(out_img, (out_x + 10, out_y + 10), out_img)
        
    if out_cnt > 1:
        c_str = str(out_cnt)
        draw.text((out_x + out_size - 28, out_y + out_size - 30), c_str, fill=(40, 40, 40), font=font_count)
        draw.text((out_x + out_size - 30, out_y + out_size - 32), c_str, fill=(255, 255, 255), font=font_count)
        
    out_path = os.path.join(RECIPES_DIR, filename)
    img.save(out_path)
    print(f"Generated: {filename}")


# 1. Storage Component
render_recipe_image(
    "Storage Component",
    "Base crafting material used in all network blocks and upgrades",
    [
        "iron_ingot", "chest_wood", "iron_ingot",
        "chest_wood", "redstone",   "chest_wood",
        "iron_ingot", "chest_wood", "iron_ingot"
    ],
    "storage_component", 1,
    "crafting_storage_component.png"
)

# 2. Storage Heart
render_recipe_image(
    "Storage Heart",
    "Core network engine with 20-slot station inventory and Light Level 15",
    [
        "diamond",        "sea_lantern",       "diamond",
        "emerald",        "storage_component", "emerald",
        "redstone_block", "redstone_block",    "redstone_block"
    ],
    "storage_heart", 1,
    "crafting_storage_heart.png"
)

# 3. Storage Access
render_recipe_image(
    "Storage Access Terminal",
    "Terminal to browse, search, extract, and insert items across the network",
    [
        "iron_ingot", "diamond",           "iron_ingot",
        "iron_ingot", "storage_component", "iron_ingot",
        "iron_ingot", "chest_wood",        "iron_ingot"
    ],
    "storage_access", 1,
    "crafting_storage_access.png"
)

# 4. Crafting Access Interface
render_recipe_image(
    "Crafting Access Interface",
    "Advanced crafting terminal with 3-state autofill and multi-station support",
    [
        "diamond",     "clock",             "diamond",
        "diamond",     "storage_component", "diamond",
        "lapis_block", "lapis_block",       "lapis_block"
    ],
    "crafting_access", 1,
    "crafting_crafting_access.png"
)

# 5. Remote Storage Access Block
render_recipe_image(
    "Remote Storage Access Block",
    "Physical network anchor required to bind wireless portable remotes",
    [
        "obsidian",    "ender_pearl",       "obsidian",
        "ender_pearl", "storage_component", "ender_pearl",
        "obsidian",    "diamond",           "obsidian"
    ],
    "remote_access", 1,
    "crafting_remote_access.png"
)

# 6. Hell Bricks
render_recipe_image(
    "Hell Bricks (x8)",
    "Defensive building block that ignites walking entities for 3 seconds",
    [
        "netherbrick", "netherbrick", "netherbrick",
        "netherbrick", "magma",       "netherbrick",
        "netherbrick", "netherbrick", "netherbrick"
    ],
    "hell_brick", 8,
    "crafting_hell_bricks.png"
)

# 7. Basic Storage Unit
render_recipe_image(
    "Basic Storage Unit",
    "Tier 1 storage unit with 36-slot capacity (upgradable in-place)",
    [
        "log_wood",   "chest_wood",        "log_wood",
        "chest_wood", "storage_component", "chest_wood",
        "log_wood",   "chest_wood",        "log_wood"
    ],
    "storage_unit_basic", 1,
    "crafting_storage_unit_basic.png"
)

# 8. Crimtane Storage Unit
render_recipe_image(
    "Crimtane Storage Unit",
    "Tier 2 storage unit with 54-slot capacity (6 rows)",
    [
        None, "upgrade_crimtane",  None,
        None, "storage_unit_basic", None,
        None, None,                None
    ],
    "storage_unit_crimtane", 1,
    "crafting_storage_unit_crimtane.png"
)

# 9. Demonite Storage Unit
render_recipe_image(
    "Demonite Storage Unit",
    "Tier 2 storage unit with 54-slot capacity (6 rows)",
    [
        None, "upgrade_demonite",  None,
        None, "storage_unit_basic", None,
        None, None,                None
    ],
    "storage_unit_demonite", 1,
    "crafting_storage_unit_demonite.png"
)

# 10. Hellstone Storage Unit
render_recipe_image(
    "Hellstone Storage Unit",
    "Tier 3 storage unit with 90-slot capacity (10 rows)",
    [
        None, "upgrade_hellstone",     None,
        None, "storage_unit_basic",    None,
        None, None,                    None
    ],
    "storage_unit_hellstone", 1,
    "crafting_storage_unit_hellstone.png"
)

# 11. Hallowed Storage Unit
render_recipe_image(
    "Hallowed Storage Unit",
    "Tier 4 storage unit with 135-slot capacity (15 rows)",
    [
        None, "upgrade_hallowed",       None,
        None, "storage_unit_basic",     None,
        None, None,                     None
    ],
    "storage_unit_hallowed", 1,
    "crafting_storage_unit_hallowed.png"
)

# 12. Blue Chlorophyte Storage Unit
render_recipe_image(
    "Blue Chlorophyte Storage Unit",
    "Tier 5 storage unit with 180-slot capacity (20 rows)",
    [
        None, "upgrade_blue_chlorophyte", None,
        None, "storage_unit_basic",        None,
        None, None,                        None
    ],
    "storage_unit_blue_chlorophyte", 1,
    "crafting_storage_unit_blue_chlorophyte.png"
)

# 13. Luminite Storage Unit
render_recipe_image(
    "Luminite Storage Unit",
    "Tier 6 storage unit with 252-slot capacity (28 rows)",
    [
        None, "upgrade_luminite",      None,
        None, "storage_unit_basic",     None,
        None, None,                    None
    ],
    "storage_unit_luminite", 1,
    "crafting_storage_unit_luminite.png"
)

# 14. Terra Storage Unit
render_recipe_image(
    "Terra Storage Unit",
    "Tier 7 storage unit with 360-slot capacity (40 rows)",
    [
        None, "upgrade_terra",          None,
        None, "storage_unit_basic",     None,
        None, None,                     None
    ],
    "storage_unit_terra", 1,
    "crafting_storage_unit_terra.png"
)

# 15. Crimtane Storage Upgrade
render_recipe_image(
    "Crimtane Storage Upgrade",
    "Shift+Right-Click any lower-tier Unit to upgrade to Crimtane (54 slots)",
    [
        "ingot_crimtane", "iron_block",        "ingot_crimtane",
        "redstone_block", "storage_component", "redstone_block",
        "ingot_crimtane", "iron_block",        "ingot_crimtane"
    ],
    "upgrade_crimtane", 1,
    "crafting_upgrade_crimtane.png"
)

# 16. Demonite Storage Upgrade
render_recipe_image(
    "Demonite Storage Upgrade",
    "Shift+Right-Click any lower-tier Unit to upgrade to Demonite (54 slots)",
    [
        "ingot_demonite", "iron_block",        "ingot_demonite",
        "obsidian",       "storage_component", "obsidian",
        "ingot_demonite", "iron_block",        "ingot_demonite"
    ],
    "upgrade_demonite", 1,
    "crafting_upgrade_demonite.png"
)

# 17. Hellstone Storage Upgrade
render_recipe_image(
    "Hellstone Storage Upgrade",
    "Shift+Right-Click any lower-tier Unit to upgrade to Hellstone (90 slots)",
    [
        "bar_hellstone", "blaze_rod",         "bar_hellstone",
        "hell_brick",    "storage_component", "hell_brick",
        "bar_hellstone", "blaze_rod",         "bar_hellstone"
    ],
    "upgrade_hellstone", 1,
    "crafting_upgrade_hellstone.png"
)

# 18. Hallowed Storage Upgrade
render_recipe_image(
    "Hallowed Storage Upgrade",
    "Shift+Right-Click any lower-tier Unit to upgrade to Hallowed (135 slots)",
    [
        "ingot_hallowed", "diamond_block",     "ingot_hallowed",
        "ghast_tear",     "storage_component", "ghast_tear",
        "ingot_hallowed", "diamond_block",     "ingot_hallowed"
    ],
    "upgrade_hallowed", 1,
    "crafting_upgrade_hallowed.png"
)

# 19. Blue Chlorophyte Storage Upgrade
render_recipe_image(
    "Blue Chlorophyte Storage Upgrade",
    "Shift+Right-Click any lower-tier Unit to upgrade to Chlorophyte (180 slots)",
    [
        "bar_chlorophyte", "lapis_block",       "bar_chlorophyte",
        "sea_lantern",     "storage_component", "sea_lantern",
        "bar_chlorophyte", "lapis_block",       "bar_chlorophyte"
    ],
    "upgrade_blue_chlorophyte", 1,
    "crafting_upgrade_blue_chlorophyte.png"
)

# 20. Luminite Storage Upgrade
render_recipe_image(
    "Luminite Storage Upgrade",
    "Shift+Right-Click any lower-tier Unit to upgrade to Luminite (252 slots)",
    [
        "bar_luminite", "radiant_jewel",     "bar_luminite",
        "bar_luminite", "storage_component", "bar_luminite",
        "bar_luminite", "shulker_shell",     "bar_luminite"
    ],
    "upgrade_luminite", 1,
    "crafting_upgrade_luminite.png"
)

# 21. Terra Storage Upgrade
render_recipe_image(
    "Terra Storage Upgrade",
    "Shift+Right-Click any lower-tier Unit to upgrade to Terra (360 slots)",
    [
        "radiant_jewel", "terra_singularity",  "radiant_jewel",
        "bar_luminite",  "storage_component",  "bar_luminite",
        "end_crystal",   "nether_star",        "end_crystal"
    ],
    "upgrade_terra", 1,
    "crafting_upgrade_terra.png"
)

# 22. Basic Portable Storage Access (PreHM)
render_recipe_image(
    "Basic Portable Storage Access",
    "Wireless storage access remote (Range: 200 blocks, same dimension)",
    [
        "gold_ingot",   "shadow_diamond", "gold_ingot",
        "ender_pearl",  "remote_access",  "ender_pearl",
        "diamond",      "storage_access", "diamond"
    ],
    "portableaccessprehm", 1,
    "crafting_portable_access_prehm.png"
)

# 23. Advanced Portable Storage Access (HM)
render_recipe_image(
    "Advanced Portable Storage Access",
    "Wireless storage access remote (Range: Unlimited, same dimension)",
    [
        "shulker_shell",       "ingot_hallowed",    "shulker_shell",
        "chorus_fruit_popped", "portableaccessprehm", "chorus_fruit_popped",
        "diamond_block",       "ender_eye",         "diamond_block"
    ],
    "portableaccesshm", 1,
    "crafting_portable_access_hm.png"
)

# 24. Ultimate Portable Storage Access
render_recipe_image(
    "Ultimate Portable Storage Access",
    "Wireless storage access remote (Range: Unlimited, Cross-Dimensional)",
    [
        "dragon_breath", "radiant_jewel",   "dragon_breath",
        "bar_luminite",  "portableaccesshm", "bar_luminite",
        "end_crystal",   "terra_singularity", "end_crystal"
    ],
    "portableaccess", 1,
    "crafting_portable_access_ultimate.png"
)

# 25. Basic Portable Crafting Access (PreHM)
render_recipe_image(
    "Basic Portable Crafting Access",
    "Wireless crafting terminal remote (Range: 200 blocks, same dimension)",
    [
        "gold_ingot",   "shadow_diamond",  "gold_ingot",
        "ender_pearl",  "remote_access",   "ender_pearl",
        "diamond",      "crafting_access", "diamond"
    ],
    "portablecraftingaccessprehm", 1,
    "crafting_portable_crafting_access_prehm.png"
)

# 26. Advanced Portable Crafting Access (HM)
render_recipe_image(
    "Advanced Portable Crafting Access",
    "Wireless crafting terminal remote (Range: Unlimited, same dimension)",
    [
        "shulker_shell",       "ingot_hallowed",            "shulker_shell",
        "chorus_fruit_popped", "portablecraftingaccessprehm", "chorus_fruit_popped",
        "diamond_block",       "ender_eye",                 "diamond_block"
    ],
    "portablecraftingaccesshm", 1,
    "crafting_portable_crafting_access_hm.png"
)

# 27. Ultimate Portable Crafting Access
render_recipe_image(
    "Ultimate Portable Crafting Access",
    "Wireless crafting terminal remote (Range: Unlimited, Cross-Dimensional)",
    [
        "dragon_breath", "radiant_jewel",           "dragon_breath",
        "bar_luminite",  "portablecraftingaccesshm", "bar_luminite",
        "end_crystal",   "terra_singularity",         "end_crystal"
    ],
    "portablecraftingaccess", 1,
    "crafting_portable_crafting_access_ultimate.png"
)

# 28. Shadow Diamond
render_recipe_image(
    "Shadow Diamond",
    "Abyssal diamond infused with Demonite & Crimtane shadow magic",
    [
        "diamond",        "obsidian",       "obsidian",
        "ingot_demonite", "ingot_crimtane", None,
        None,               None,               None
    ],
    "shadow_diamond", 1,
    "crafting_shadow_diamond.png"
)

# 29. Radiant Jewel
render_recipe_image(
    "Radiant Jewel",
    "Celestial prism radiating stellar energy from Luminite & the End",
    [
        "nether_star",   "bar_luminite", "bar_luminite",
        "dragon_breath", "end_crystal",  None,
        None,              None,             None
    ],
    "radiant_jewel", 1,
    "crafting_radiant_jewel.png"
)

# 30. Terra Singularity
render_recipe_image(
    "Terra Singularity",
    "Cosmic core synthesis of all 6 progression metals and bosses",
    [
        "ingot_demonite", "ingot_crimtane",   "bar_hellstone",
        "ingot_hallowed", "bar_chlorophyte",  "bar_luminite",
        "nether_star",    "dragon_breath",    "end_crystal"
    ],
    "terra_singularity", 1,
    "crafting_terra_singularity.png"
)

# 31. Hellstone Bar
render_recipe_image(
    "Hellstone Bar",
    "Forged in Storage Heart with Furnace from Nether Hellstone Fragments",
    [
        "fragment_hellstone", "fragment_hellstone", "fragment_hellstone",
        "obsidian",           "blaze_powder",       None,
        None,                   None,                   None
    ],
    "bar_hellstone", 1,
    "crafting_bar_hellstone.png"
)

# 32. Hallowed Ingot (x2)
render_recipe_image(
    "Hallowed Ingot (x2)",
    "Sacred metal crafted with Gold, Diamond, Glowstone, and Quartz (slots 3 & 6)",
    [
        "gold_ingot", "diamond",        "quartz",
        "gold_ingot", "glowstone_dust", "quartz",
        None,           None,               None
    ],
    "ingot_hallowed", 2,
    "crafting_ingot_hallowed.png"
)

# 33. Luminite Bar
render_recipe_image(
    "Luminite Bar",
    "Refined in Storage Heart with Furnace from celestial End Luminite Fragments",
    [
        "fragment_luminite", "fragment_luminite", "fragment_luminite",
        "fragment_luminite", None,                  None,
        None,                  None,                  None
    ],
    "bar_luminite", 1,
    "crafting_bar_luminite.png"
)

# 34. Chlorophyte Bar (x2)
render_recipe_image(
    "Chlorophyte Bar (x2)",
    "Living botanical alloy crafted with Emeralds, Prismarine, and Gold",
    [
        "emerald",          "emerald",          "prismarine_shard",
        "prismarine_shard", "gold_ingot",       None,
        None,                 None,                 None
    ],
    "bar_chlorophyte", 2,
    "crafting_bar_chlorophyte.png"
)

# 35. Crimtane Ingot (x2)
render_recipe_image(
    "Crimtane Ingot (x2)",
    "Flesh-carved metal forged with Iron, Nether Wart, and Redstone",
    [
        "iron_ingot",   "iron_ingot", "nether_wart",
        "redstone",     None,           None,
        None,             None,           None
    ],
    "ingot_crimtane", 2,
    "crafting_ingot_crimtane.png"
)

# 36. Demonite Ingot (x2)
render_recipe_image(
    "Demonite Ingot (x2)",
    "Shadow-infused metal forged with Iron, Obsidian, and Ender Pearl",
    [
        "iron_ingot",   "iron_ingot", "obsidian",
        "ender_pearl",  None,           None,
        None,             None,           None
    ],
    "ingot_demonite", 2,
    "crafting_ingot_demonite.png"
)

# 37. Furnace Smelting Demonstration
render_recipe_image(
    "Furnace Smelting: Mass Iron Smelt",
    "Center fuel powers 8 surrounding identical ores (Probabilistic burn calculation)",
    [
        ("iron_ore", 1), ("iron_ore", 1), ("iron_ore", 1),
        ("iron_ore", 1), ("coal", 1),     ("iron_ore", 1),
        ("iron_ore", 1), ("iron_ore", 1), ("iron_ore", 1)
    ],
    ("iron_ingot", 8), 8,
    "station_furnace_smelting.png"
)

# 38. Enchanting Table Demonstration
render_recipe_image(
    "Enchanting Table: Tier 30 Enchanting",
    "Item in Slot 1, Lapis in Slots 4, 5, 6. Bookshelf power read from network items!",
    [
        "diamond_sword", None, None,
        ("lapis_lazuli", 1), ("lapis_lazuli", 1), ("lapis_lazuli", 1),
        None, None, None
    ],
    "diamond_sword", 1,
    "station_enchanting_table.png"
)

# 39. Anvil Repair Demonstration
render_recipe_image(
    "Anvil Repair: Tool & Material",
    "Place damaged item in Slot 1 and repair material in Slot 5. Enforces XP level cost.",
    [
        "diamond_pickaxe", None, None,
        None, "diamond", None,
        None, None, None
    ],
    "diamond_pickaxe", 1,
    "station_anvil_repair.png"
)

# 40. Anvil Combine Demonstration
render_recipe_image(
    "Anvil Combine: Sword + Enchanted Book",
    "Combines enchants and computes authentic anvil XP level costs.",
    [
        "diamond_sword", None, None,
        None, "enchanted_book", None,
        None, None, None
    ],
    "diamond_sword", 1,
    "station_anvil_combine.png"
)

# 41. Brewing Stand Demonstration
render_recipe_image(
    "Brewing Stand: Batch Awkward Potion",
    "Blaze powder in Slot 1, Nether wart in Slot 2, up to 3 water bottles in Slots 4,5,6.",
    [
        "blaze_powder", "nether_wart", None,
        "glass_bottle", "glass_bottle", "glass_bottle",
        None, None, None
    ],
    ("awkward_potion", 3), 3,
    "station_brewing_stand.png"
)

# 42. Rustic Simple Alchemy Demonstration
render_recipe_image(
    "Rustic Simple Alchemy: Elixir of Healing",
    "Fuel in Slot 5 (400 ticks minimum), Water Bucket, Chamomile, Cohosh, Glass Bottle.",
    [
        "chamomile", "cohosh", "glass_bottle",
        "water_bucket", "coal", None,
        None, None, None
    ],
    "elixir", 1,
    "station_rustic_alchemy_simple.png"
)

# 43. Rustic Advanced Alchemy Demonstration
render_recipe_image(
    "Rustic Advanced Alchemy: Elixir of Iron Skin",
    "Fuel in Slot 5 (300 ticks minimum), Water Bucket, Aloe Vera, Blood Orchid, Bottle.",
    [
        "aloe_vera", "blood_orchid", "glass_bottle",
        "water_bucket", "coal", None,
        None, None, None
    ],
    "elixir", 1,
    "station_rustic_alchemy_advanced.png"
)

# 44. Rustic Brewing Barrel Demonstration
render_recipe_image(
    "Rustic Brewing Barrel: Quality Wine Brewing",
    "Fruits + Sugar + Optional modifier wine bottle in Slot 5 (Preserves quality!).",
    [
        ("grapes", 4), "sugar", None,
        None, "wine", None,
        None, None, None
    ],
    "wine", 1,
    "station_rustic_brewing_barrel.png"
)

# 45. Rustic Crushing Tub Demonstration
render_recipe_image(
    "Rustic Crushing Tub: Grape Juice",
    "Slot 1: 4 Fruits (Grapes/Wildberries), Slot 5: Empty Glass Bottle. Enforces 4 fruits.",
    [
        ("grapes", 4), None, None,
        None, "glass_bottle", None,
        None, None, None
    ],
    "grape_juice", 1,
    "station_rustic_crushing_tub.png"
)

# 46. Disenchanter Table Demonstration
render_recipe_image(
    "Disenchanter: Enchantment Extraction",
    "Enchanted item in Slot 5, Book in Slot 3. Extracts enchantments onto book!",
    [
        None, None, "book",
        None, "diamond_sword", None,
        None, None, None
    ],
    "enchanted_book", 1,
    "station_disenchanter.png"
)

# 47. Bountiful Baubles Reforger Demonstration
render_recipe_image(
    "Bountiful Baubles: Modifier Reforging",
    "Place bauble item in Slot 9. Reforges modifier attributes using player XP levels.",
    [
        None, None, None,
        None, None, None,
        None, None, "ring_overclocking"
    ],
    "ring_overclocking", 1,
    "station_bountiful_baubles_reforger.png"
)

# 48. Quality Tools Reforging Station Demonstration
render_recipe_image(
    "Quality Tools: Tool & Armor Reforging",
    "Item in Slot 5, Reforging Material in Slot 9. Supports all 500+ dynamic materials!",
    [
        None, None, None,
        None, "bezoar", None,
        None, None, "spectral_silt"
    ],
    "bezoar", 1,
    "station_quality_tools_reforge.png"
)

print("All 48 recipes rendered successfully!")
