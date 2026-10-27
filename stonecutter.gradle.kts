stonecutter active "26.2"

stonecutter parameters {
    properties {
        tags(current.version)
    }

    files {
        disable(current.version != "1.20.1", "java/**/gui/PlayerBodyWidget.java")
        disable(current.parsed >= "26.1", "java/**/gui/button/ItemIconButton.java")
        disable(current.parsed < "1.20.5", "java/**/network/payload/*.java")
        disable(current.parsed < "1.20.5", "java/**/modmenu/UpdateInfoImpl.java")
    }

    replacements {
        string(current.parsed >= "1.21.2") {
            replace("mc.tell(", "mc.schedule(")
        }
        string(current.parsed >= "1.21.9") {
            include("java/**/mixin/NametagRendererMixin.java")
            replace("PlayerRenderer", "AvatarRenderer")
            replace("PlayerRenderState", "AvatarRenderState")
        }
        string(current.parsed >= "1.21.9") {
            replace("client.resources.PlayerSkin", "world.entity.player.PlayerSkin")
            replace(".getProfile().getName()", ".getProfile().name()")
        }
        string(current.parsed >= "1.21.11") {
            replace("ResourceLocation", "Identifier")
        }
        string(current.parsed >= "26.1") {
            replace("GuiGraphics", "GuiGraphicsExtractor")
            replace("ClientCommandManager", "ClientCommands")
            replace("graphics.drawString(", "graphics.text(")
            replace("graphics.drawCenteredString(", "graphics.centeredText(")
            replace("PlayerFaceRenderer.draw(", "PlayerFaceExtractor.extractRenderState(")
            replace("PlayerFaceRenderer", "PlayerFaceExtractor")
        }
        string(current.parsed >= "26.2") {
            replace("mc.setScreen(", "mc.gui.setScreen(")
        }
    }
}
