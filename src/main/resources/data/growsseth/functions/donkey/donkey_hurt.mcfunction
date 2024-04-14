#give advancement only if near researcher
execute as @e[type=growsseth:researcher] at @s if entity @e[type=donkey,nbt={Tags:["researcher_donkey"]},distance=0..20] run advancement grant @p only growsseth:donkey/donkey_hurt_dialogue
#execute as @e[type=growsseth:researcher] at @s if entity @e[type=donkey,nbt={Tags:["researcher_donkey"]},distance=0..20] run say donkey hurt

advancement revoke @p only growsseth:donkey/donkey_hurt
advancement revoke @p only growsseth:donkey/donkey_hurt_dialogue