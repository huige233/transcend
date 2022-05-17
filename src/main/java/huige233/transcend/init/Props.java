package huige233.transcend.init;

import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.util.EnumFacing;

public class Props {
    public static final PropertyEnum<EnumFacing> HORIZONTAL_FACING = PropertyEnum.create("facing", EnumFacing.class, (facing) -> {
        return facing.getAxis() != EnumFacing.Axis.Y;
    });
    public static final PropertyBool ACTIVE = PropertyBool.create("active");

    public Props() {
    }
}
