package com.ruslan.growsseth.mixin.client.growssethworld;

import com.ruslan.growsseth.client.gui.RawSetEditBox;
import net.minecraft.client.gui.components.EditBox;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.function.Consumer;

@Mixin(EditBox.class)
public abstract class EditBoxMixin implements RawSetEditBox {
    @Shadow @Nullable
    private Consumer<String> responder;

    @Shadow public abstract void setValue(String text);

    @Override
    public void growsseth_rawSetValue(String value) {
        EditBox th1s = (EditBox) (Object) this;
        Consumer<String> responderTemp = this.responder;
        this.responder = null;
        this.setValue(value);
        this.responder = responderTemp;
    }
}
