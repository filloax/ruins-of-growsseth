package com.ruslan.growsseth.mixin.client;

import com.llamalad7.mixinextras.sugar.Local;
import com.ruslan.growsseth.entity.researcher.Researcher;
import net.minecraft.client.model.VillagerModel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(VillagerModel.class)
public abstract class VillagerModelMixin {
    // Errors are normal apparently
    @ModifyVariable(
        at = @At(value = "STORE", ordinal = 0),
        method = "setupAnim", ordinal = 0
    )
    private boolean researcherUnhappyCheck(boolean bl, @Local(argsOnly = true) Entity entity) {
        if (entity instanceof Researcher researcher) {
            return researcher.getUnhappyCounter() > 0;
        }
        return bl;
    }

    /*****************************************************************************************************************/
    /*         Target Class : net.minecraft.client.model.VillagerModel                                               */
    /*        Target Method : setupAnim                                                                              */
    /*        Callback Name : researcherUnhappyCheck                                                                 */
    /*         Capture Type : VillagerModel                                                                          */
    /*          Instruction : [52] MethodInsnNode INVOKESTATIC                                                       */
    /*****************************************************************************************************************/
    /*           Match mode : IMPLICIT (match single) - INVALID (0 matches)                                          */
    /*        Match ordinal : any                                                                                    */
    /*          Match index : any                                                                                    */
    /*        Match name(s) : any                                                                                    */
    /*            Args only : false                                                                                  */
    /*****************************************************************************************************************/
    /* INDEX  ORDINAL                            TYPE  NAME                                                CANDIDATE */
    /* [  1]    [  0]                          Entity  entity                                              -         */
    /* [  2]    [  0]                           float  limbSwing                                           -         */
    /* [  3]    [  1]                           float  limbSwingAmount                                     -         */
    /* [  4]    [  2]                           float  ageInTicks                                          -         */
    /* [  5]    [  3]                           float  netHeadYaw                                          -         */
    /* [  6]    [  4]                           float  headPitch                                           -         */
    /* [  7]    [  0]                         boolean  bl                                                  -         */
    /*****************************************************************************************************************/
}
