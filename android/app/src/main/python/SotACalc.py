import os
import numpy as np
import pandas as pd

def load_game_data():
    current_dir = os.path.dirname(__file__)
    
    exp_path = os.path.join(current_dir, "Shroud_Exp_by_Skill_Level_Cleaned.csv")
    exp_by_skill_level = pd.read_csv(exp_path, header=None).values.flatten()
    
    skills_path = os.path.join(current_dir, "SkillExp_Adventuring_Skills_Cleaned.csv")
    df = pd.read_csv(skills_path)
    
    # Normalize column names to prevent key mismatches
    skill_modifiers = pd.DataFrame({
        'skill': df['Skill_Name'].astype(str).str.lower(),
        'modifier': df['Skill_Multiplier'].astype(float)
    })
    
    return exp_by_skill_level, skill_modifiers
    
def load_game_data_jupyter(exp_csv_path='Shroud_Exp_by_Skill_Level_Cleaned.csv', 
                   modifiers_csv_path='SkillExp_Adventuring_Skills_Cleaned.csv'):
    """Loads and returns the experience levels and skill modifier dataframes."""
    # Load the pre-cleaned experience CSV file directly as integers[cite: 1]
    exp_df = pd.read_csv(exp_csv_path, header=None, dtype=int)
    
    # Convert the DataFrame column into a NumPy array[cite: 1]
    exp_by_skill_level = exp_df.values.flatten()

    # Load skill modifiers
    df = pd.read_csv(modifiers_csv_path)
    skills = df['Skill_Name'].astype(str).str.lower()
    modifiers = df['Skill_Multiplier'].astype(float)

    skill_modifiers = pd.DataFrame({
        'skill': skills,
        'modifier': modifiers
    })

    return exp_by_skill_level, skill_modifiers


def calc_skill_exp_at_level(skill, level, exp_by_skill_level, skill_modifiers):
    """Calculates experience required for a specific skill at a given level."""
    modifier = 0
    col_name = 'skill' if 'skill' in skill_modifiers.columns else 'Skill_Name'
    mod_col = 'modifier' if 'modifier' in skill_modifiers.columns else 'Skill_Multiplier'
    
    for i in range(len(skill_modifiers)):
        if skill.lower() == str(skill_modifiers[col_name][i]).lower():
            modifier = float(skill_modifiers[mod_col][i])
            break
            
    # Clamp level safely between 1 and the maximum available index length
    max_level_index = len(exp_by_skill_level) - 1
    safe_level_idx = max(0, min(level - 1, max_level_index))
    
    return int(exp_by_skill_level[safe_level_idx] * modifier)

# Magic schools mapping definition[cite: 1]
MAGIC_SCHOOLS = {
    "Air"   : ["Air Magic Specialization", "Air's Embrace", "Blink", "Chain Lightning", "Dash", "Discharge", "Earth Warding", "Gust", "Lightning", "Lightning Storm", "Rapid Discharge", "Shield of Air", "Summon Air Elemental", "Well Grounded"],
    "Chaos" : ["Confusion", "Chaos Bolt", "Juxtapose", "Chaotic Clone", "Torpid Torment", "Tabula Rasa", "Chaotic Step", "Chaotic Shield", "Chaotic Feedback", "Summon Daemon", "Chaos Magic Specialization"],
    "Death" : ["Tap Soul", "Corpse Explosion", "Death Touch", "Death Mastery", "Summon Skeleton", "Death Ray", "Undead Mastery", "Mummified Undead", "Death Shield", "Death Field", "Death Magic Specialization", "Life Warding", "Summon Lich"],
    "Earth" : ["Stone Fist", "Root", "Stone Arrow", "Strength of Earth", "Sympathy of Stone", "Shield of Crystal", "Earthquake", "Earth's Embrace", "Bulwark of Earth", "Summon Earth Elemental", "Torpor", "Earth Magic Specialization", "Air Warding"],
    "Fire"  : ["Flame Fist", "Fire Arrow", "Ignite Weapon", "Highly Flammable", "Fireball", "Immolation", "Fire Speed", "Fire Proof", "Ring of Fire", "Fire Magic Specialization", "Water Warding", "Summon Fire Elemental"],
    "Life"  : ["Healing Touch", "Healing Ray", "Purify", "Beneficent Blessing", "Healing Burst", "Purify Burst", "Banish Undead", "Life Power", "Life Reach", "Healing Grace", "Resurrection", "Life Magic Specialization", "Death Warding", "Healing Blast"],
    "Moon"  : ["Night Vision", "Vanish", "Celestial Blessing", "Fireflies", "Animal Charm", "Moon Worship", "Shadow Form", "Cocoon of Night", "Meteor Shower", "Shooting Star", "Moon Beams", "Moon Eater", "Night Step", "Summon Will-O-Wisp", "Lunar Magic Specialization", "Sun Warding"],
    "Sun"   : ["Light", "Searing Ray", "Enlightenment", "Blind", "Sun Worship", "Reveal Hidden", "Dazzling Rays", "Sun Eater", "Summon Phoenix", "Elysian Illumination", "Solar Flare", "Sun Magic Specialization", "Moon Warding"],
    "Water" : ["Ice Fist", "Ice Arrow", "Douse", "Icy Breath", "Ice Field", "Soothing Rain", "Shield of Ice", "Blue Blooded", "Summon Water Elemental", "Cone of Cold", "Water Magic Specialization", "Fire Warding", "Summon Ice Elemental"]
}


def optimize_skill_levels(skills_to_optimize, target_attunement, exp_by_skill_level, skill_modifiers):
    """Runs the optimization algorithm to find the ideal skill level distribution safely."""
    if hasattr(skills_to_optimize, "toArray"):
        raw_list = skills_to_optimize.toArray()
    else:
        raw_list = skills_to_optimize
        
    skills_to_optimize = []
    for item in raw_list:
        skills_to_optimize.append({
            'skill': str(item.get('skill') if hasattr(item, "get") else item.get('skill')),
            'locked': bool(item.get('locked') if hasattr(item, "get") else item.get('locked')),
            'locked_value': int(item.get('locked_value')) if hasattr(item, "get") and item.get('locked_value') is not None else 0
        })

    assert target_attunement >= 1 and target_attunement <= 200, "Target attunement must be between 1 and 200."
    assert len(skills_to_optimize) == 10, "Attunement is the average of your top ten skills. We need ten skills to optimize."
    
    for i in range(len(skills_to_optimize)):
        if skills_to_optimize[i]["locked"]:
            val = skills_to_optimize[i]["locked_value"]
            assert val >= 1 and val <= 200, "Skills must be locked between 1 and 200"
    
    all_skills_in_same_school = False
    for school in MAGIC_SCHOOLS:
        all_skills_found = True
        for i in range(len(skills_to_optimize)):
            if skills_to_optimize[i]['skill'] not in MAGIC_SCHOOLS[school]:
                all_skills_found = False
                break
        if all_skills_found:
            all_skills_in_same_school = True
            break
            
    assert all_skills_in_same_school is True, "All skills must belong to the same magic school!"
        
    
    
    optimization_data = {}
    points_remaining = target_attunement * 10
    unlocked_indices = []
    for i in range(len(skills_to_optimize)):
        if skills_to_optimize[i]['locked']:
            optimization_data[skills_to_optimize[i]['skill']] = skills_to_optimize[i]['locked_value']
            points_remaining -= skills_to_optimize[i]['locked_value']
        else:
            unlocked_indices.append(i)

    unlocked_count = len(unlocked_indices)
    assert points_remaining / unlocked_count <= 200.0, "Target attunement impossible to reach with these locked skills, as it would require unlocked skills to be greater than 200."
    base_skill_value = points_remaining // unlocked_count
    remainder = points_remaining % unlocked_count
    count = 0
    for i in unlocked_indices:
        skill_value = base_skill_value
        if count < remainder:
            skill_value += 1
        optimization_data[skills_to_optimize[i]['skill']] = skill_value
        count += 1

    total_skill_points = 0
    skill_count = 0
    for skill in optimization_data:
        skill_count += 1
        total_skill_points += optimization_data[skill]
    assert total_skill_points == 10 * target_attunement, f"The total skill points ({total_skill_points}) need to add up to 10 * target attunement."

    finished_optimization = False
    iteration_count = 0
    max_iterations = 5000  # Safety threshold to prevent UI freezes
    
    while not finished_optimization and iteration_count < max_iterations:
        iteration_count += 1
        skill_most_to_gain_from_dropping = None
        max_gain = 0
        skill_least_cost_to_raise = None
        min_cost = float('inf')
        
        for skill_key in optimization_data:
            skill_str = str(skill_key)
            skip = False
            for i in range(len(skills_to_optimize)):
                if skills_to_optimize[i]['skill'] == skill_str and skills_to_optimize[i]['locked']:
                    skip = True
                    break
            if skip:
                continue
                
            current_lvl = optimization_data[skill_key]
            skill_exp_last = calc_skill_exp_at_level(skill_str, current_lvl - 1, exp_by_skill_level, skill_modifiers)
            skill_exp = calc_skill_exp_at_level(skill_str, current_lvl, exp_by_skill_level, skill_modifiers)
            skill_exp_next = calc_skill_exp_at_level(skill_str, current_lvl + 1, exp_by_skill_level, skill_modifiers)

            cost_to_level_up = skill_exp_next - skill_exp
            if cost_to_level_up < min_cost:
                min_cost = cost_to_level_up
                skill_least_cost_to_raise = skill_key

            gain_from_drop = skill_exp - skill_exp_last
            if gain_from_drop > max_gain:
                max_gain = gain_from_drop
                skill_most_to_gain_from_dropping = skill_key

        # Strict inequality prevents infinite oscillation loops when costs match
        if max_gain <= min_cost or skill_most_to_gain_from_dropping is None or skill_least_cost_to_raise is None:
            finished_optimization = True
        else:
            # Prevent swapping if it hits boundary limits (1 or 200)
            if optimization_data[skill_most_to_gain_from_dropping] <= 1 or optimization_data[skill_least_cost_to_raise] >= 200:
                finished_optimization = True
            else:
                optimization_data[skill_most_to_gain_from_dropping] -= 1
                optimization_data[skill_least_cost_to_raise] += 1

    total_exp_required = 0
    results = {}
    for skill_key in optimization_data:
        skill_str = str(skill_key)
        results[skill_str] = optimization_data[skill_key]
        total_exp_required += calc_skill_exp_at_level(skill_str, optimization_data[skill_key], exp_by_skill_level, skill_modifiers)
        
    return results, total_exp_required