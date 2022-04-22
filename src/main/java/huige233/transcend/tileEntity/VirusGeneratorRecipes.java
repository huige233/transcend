package huige233.transcend.tileEntity;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Maps;
import com.google.common.collect.Table;
import huige233.transcend.init.ModItems;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

import java.util.Map;
import java.util.Map.Entry;
public class VirusGeneratorRecipes {
    private static final VirusGeneratorRecipes INSTANCE = new VirusGeneratorRecipes();
    private final Table<ItemStack, ItemStack, ItemStack> smeltingList = HashBasedTable.<ItemStack, ItemStack, ItemStack>create();
    private final Map<ItemStack, Float> experienceList = Maps.<ItemStack, Float>newHashMap();

    public static VirusGeneratorRecipes getInstance()
    {
        return INSTANCE;
    }

    private VirusGeneratorRecipes()
    {
        addGeneratorRecipe(new ItemStack(Items.NETHER_STAR),new ItemStack(ModItems.BEDROCK_LI),new ItemStack(ModItems.TRANSCEND),200.0f);
        //addGeneratorRecipe(new ItemStack(Items.ROTTEN_FLESH), new ItemStack(Items.GOLD_INGOT), new ItemStack(Items.NETHER_STAR), 5.0F);

    }


    public void addGeneratorRecipe(ItemStack input1, ItemStack input2, ItemStack result, float experience)
    {
        if(getSinteringResult(input1, input2) != ItemStack.EMPTY) return;
        this.smeltingList.put(input1, input2, result);
        this.experienceList.put(result, Float.valueOf(experience));
    }

    public ItemStack getSinteringResult(ItemStack input1, ItemStack input2)
    {
        for(Entry<ItemStack, Map<ItemStack, ItemStack>> entry : this.smeltingList.columnMap().entrySet())
        {
            if(this.compareItemStacks(input1, (ItemStack)entry.getKey()))
            {
                for(Entry<ItemStack, ItemStack> ent : entry.getValue().entrySet())
                {
                    if(this.compareItemStacks(input2, (ItemStack)ent.getKey()))
                    {
                        return (ItemStack)ent.getValue();
                    }
                }
            }
        }
        return ItemStack.EMPTY;
    }

    private boolean compareItemStacks(ItemStack stack1, ItemStack stack2)
    {
        return stack2.getItem() == stack1.getItem() && (stack2.getMetadata() == 32767 || stack2.getMetadata() == stack1.getMetadata());
    }

    public Table<ItemStack, ItemStack, ItemStack> getDualSmeltingList()
    {
        return this.smeltingList;
    }

    public float getGeneratorExperience(ItemStack stack)
    {
        for (Entry<ItemStack, Float> entry : this.experienceList.entrySet())
        {
            if(this.compareItemStacks(stack, (ItemStack)entry.getKey()))
            {
                return ((Float)entry.getValue()).floatValue();
            }
        }
        return 0.2F;
    }
}
