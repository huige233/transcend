package huige233.transcend;

import huige233.transcend.init.ModItems;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;

public class TranscendTab extends CreativeTabs {
    public TranscendTab()
    {
        super("transcend");
        setBackgroundImageName("transcend.png");
    }
	
	
	@Override
	public boolean hasSearchBar()
	{
		return true;
	}

    @Override
    public ItemStack createIcon()
    {
        return(new ItemStack( ModItems.TRANSCEND ) );
    }
}
