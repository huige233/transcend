const RESEARCH_NODES = [
  {
    "id": "energy_basics",
    "tier": 0,
    "pre": [],
    "cost": "1000",
    "ticks": 200,
    "name": "能量基础",
    "description": "建立发电与储能的基础模型，为后续能量网络研究打下基础。",
    "branch": 1
  },
  {
    "id": "materials_basics",
    "tier": 0,
    "pre": [],
    "cost": "1200",
    "ticks": 220,
    "name": "材料基础",
    "description": "研究结构材料的基础性质，沿材料路线逐步探索加工与强化。",
    "branch": 2
  },
  {
    "id": "automation_basics",
    "tier": 0,
    "pre": [],
    "cost": "1500",
    "ticks": 240,
    "name": "自动化基础",
    "description": "让机械按稳定流程工作。从基础控制开始，逐步探索工业自动化。",
    "branch": 3
  },
  {
    "id": "automation",
    "tier": 1,
    "pre": [
      "automation_basics"
    ],
    "cost": "100000",
    "ticks": 600,
    "name": "工业自动化",
    "description": "把独立工序组织成持续运转的流程，进一步研究精密控制。",
    "branch": 3
  },
  {
    "id": "energy_network",
    "tier": 1,
    "pre": [
      "energy_basics"
    ],
    "cost": "120000",
    "ticks": 650,
    "name": "能量网络",
    "description": "研究能量在设备之间的传输与分配，为高级储能路线建立基础。",
    "branch": 1
  },
  {
    "id": "material_processing",
    "tier": 1,
    "pre": [
      "materials_basics"
    ],
    "cost": "140000",
    "ticks": 700,
    "name": "材料加工",
    "description": "探索材料的成形与精炼过程，为更高阶材料积累研究经验。",
    "branch": 2
  },
  {
    "id": "energy_storage",
    "tier": 2,
    "pre": [
      "energy_network"
    ],
    "cost": "10000000",
    "ticks": 1200,
    "name": "高级储能",
    "description": "研究高密度储能与稳定供能，为奇点工程方向准备理论基础。",
    "branch": 1
  },
  {
    "id": "advanced_materials",
    "tier": 2,
    "pre": [
      "material_processing"
    ],
    "cost": "20000000",
    "ticks": 1300,
    "name": "先进材料",
    "description": "探索高强度、精密结构材料；这项研究同时连接材料与战斗路线。",
    "branch": 2
  },
  {
    "id": "precision_automation",
    "tier": 2,
    "pre": [
      "automation"
    ],
    "cost": "30000000",
    "ticks": 1400,
    "name": "精密自动化",
    "description": "研究更精细的流程与控制，连接自主工厂和量子知识方向。",
    "branch": 3
  },
  {
    "id": "singularity_engineering",
    "tier": 3,
    "pre": [
      "energy_storage"
    ],
    "cost": "1000000000",
    "ticks": 2400,
    "name": "奇点工程",
    "description": "围绕奇点建立高阶能源研究模型，向宇宙尺度的能源技术推进。",
    "branch": 1
  },
  {
    "id": "exotic_materials",
    "tier": 3,
    "pre": [
      "advanced_materials"
    ],
    "cost": "2000000000",
    "ticks": 2500,
    "name": "奇异材料",
    "description": "研究常规材料之外的特殊结构，为宇宙尺度的材料路线积累基础。",
    "branch": 2
  },
  {
    "id": "autonomous_factories",
    "tier": 3,
    "pre": [
      "precision_automation"
    ],
    "cost": "3000000000",
    "ticks": 2600,
    "name": "自主工厂",
    "description": "研究能够持续协调多道工序的工厂系统，向更大规模的自动化推进。",
    "branch": 3
  },
  {
    "id": "combat_basics",
    "tier": 0,
    "pre": [],
    "cost": "1800",
    "ticks": 260,
    "name": "战斗基础",
    "description": "建立武器与防护系统的基础研究，为后续战斗技术准备理论。",
    "branch": 4
  },
  {
    "id": "knowledge_basics",
    "tier": 0,
    "pre": [],
    "cost": "2000",
    "ticks": 280,
    "name": "知识基础",
    "description": "整理基础研究方法，发展后续计算与知识研究所需的理论。",
    "branch": 5
  },
  {
    "id": "advanced_combat",
    "tier": 1,
    "pre": [
      "combat_basics"
    ],
    "cost": "220000",
    "ticks": 820,
    "name": "进阶战斗",
    "description": "将基础战斗理论推进至更复杂的武器与防护研究。",
    "branch": 4
  },
  {
    "id": "advanced_knowledge",
    "tier": 1,
    "pre": [
      "knowledge_basics"
    ],
    "cost": "240000",
    "ticks": 840,
    "name": "进阶知识",
    "description": "完善研究与计算方法，向量子知识方向继续推进。",
    "branch": 5
  },
  {
    "id": "quantum_combat",
    "tier": 2,
    "pre": [
      "advanced_combat",
      "advanced_materials"
    ],
    "cost": "40000000",
    "ticks": 1500,
    "name": "量子战斗",
    "description": "结合先进材料与战斗研究，探索量子尺度的攻防技术。",
    "branch": 4
  },
  {
    "id": "quantum_knowledge",
    "tier": 2,
    "pre": [
      "advanced_knowledge",
      "precision_automation"
    ],
    "cost": "50000000",
    "ticks": 1600,
    "name": "量子知识",
    "description": "结合精密自动化与知识研究，探索更高效的计算方法。",
    "branch": 5
  },
  {
    "id": "void_combat",
    "tier": 3,
    "pre": [
      "quantum_combat",
      "singularity_engineering"
    ],
    "cost": "4000000000",
    "ticks": 2800,
    "name": "虚空战斗",
    "description": "结合量子战斗与奇点工程，探索虚空尺度的战斗理论。",
    "branch": 4
  },
  {
    "id": "strategic_knowledge",
    "tier": 3,
    "pre": [
      "quantum_knowledge",
      "autonomous_factories"
    ],
    "cost": "5000000000",
    "ticks": 3000,
    "name": "战略知识",
    "description": "结合量子知识与自主工厂，研究更大尺度的协调和决策。",
    "branch": 5
  },
  {
    "id": "cosmic_forge",
    "tier": 4,
    "pre": [
      "singularity_engineering"
    ],
    "cost": "10000000000000000000",
    "ticks": 4800,
    "name": "宇宙锻炉",
    "description": "将能源转换与储存研究推进至宇宙锻炉阶段。沿前置与后续课题，追踪本分支的时代发展。",
    "branch": 1
  },
  {
    "id": "cosmic_forge_materials",
    "tier": 4,
    "pre": [
      "exotic_materials"
    ],
    "cost": "20000000000000000000",
    "ticks": 5000,
    "name": "宇宙锻炉材料",
    "description": "将材料与结构研究推进至宇宙锻炉阶段。沿前置与后续课题，追踪本分支的时代发展。",
    "branch": 2
  },
  {
    "id": "cosmic_forge_automation",
    "tier": 4,
    "pre": [
      "autonomous_factories"
    ],
    "cost": "30000000000000000000",
    "ticks": 5200,
    "name": "宇宙锻炉自动化",
    "description": "将自动化与控制研究推进至宇宙锻炉阶段。沿前置与后续课题，追踪本分支的时代发展。",
    "branch": 3
  },
  {
    "id": "cosmic_forge_combat",
    "tier": 4,
    "pre": [
      "automation_basics"
    ],
    "cost": "40000000000000000000",
    "ticks": 5400,
    "name": "宇宙锻炉战斗",
    "description": "将武器与防护研究推进至宇宙锻炉阶段。沿前置与后续课题，追踪本分支的时代发展。",
    "branch": 4
  },
  {
    "id": "cosmic_forge_knowledge",
    "tier": 4,
    "pre": [
      "materials_basics"
    ],
    "cost": "50000000000000000000",
    "ticks": 5600,
    "name": "宇宙锻炉知识",
    "description": "将计算与知识研究推进至宇宙锻炉阶段。沿前置与后续课题，追踪本分支的时代发展。",
    "branch": 5
  },
  {
    "id": "spacetime_engineering",
    "tier": 5,
    "pre": [
      "cosmic_forge"
    ],
    "cost": "100000000000000000000000000",
    "ticks": 9600,
    "name": "时空工程",
    "description": "将能源转换与储存研究推进至时空工程阶段。沿前置与后续课题，追踪本分支的时代发展。",
    "branch": 1
  },
  {
    "id": "spacetime_engineering_materials",
    "tier": 5,
    "pre": [
      "cosmic_forge_materials"
    ],
    "cost": "200000000000000000000000000",
    "ticks": 9800,
    "name": "时空材料",
    "description": "将材料与结构研究推进至时空工程阶段。沿前置与后续课题，追踪本分支的时代发展。",
    "branch": 2
  },
  {
    "id": "spacetime_engineering_automation",
    "tier": 5,
    "pre": [
      "cosmic_forge_automation"
    ],
    "cost": "300000000000000000000000000",
    "ticks": 10000,
    "name": "时空自动化",
    "description": "将自动化与控制研究推进至时空工程阶段。沿前置与后续课题，追踪本分支的时代发展。",
    "branch": 3
  },
  {
    "id": "spacetime_engineering_combat",
    "tier": 5,
    "pre": [
      "cosmic_forge_combat"
    ],
    "cost": "400000000000000000000000000",
    "ticks": 10200,
    "name": "时空战斗",
    "description": "将武器与防护研究推进至时空工程阶段。沿前置与后续课题，追踪本分支的时代发展。",
    "branch": 4
  },
  {
    "id": "spacetime_engineering_knowledge",
    "tier": 5,
    "pre": [
      "cosmic_forge_knowledge"
    ],
    "cost": "500000000000000000000000000",
    "ticks": 10400,
    "name": "时空知识",
    "description": "将计算与知识研究推进至时空工程阶段。沿前置与后续课题，追踪本分支的时代发展。",
    "branch": 5
  },
  {
    "id": "dimensional_engineering",
    "tier": 6,
    "pre": [
      "spacetime_engineering"
    ],
    "cost": "1000000000000000000000000000000000",
    "ticks": 19200,
    "name": "维度工程",
    "description": "将能源转换与储存研究推进至维度工程阶段。沿前置与后续课题，追踪本分支的时代发展。",
    "branch": 1
  },
  {
    "id": "dimensional_engineering_materials",
    "tier": 6,
    "pre": [
      "spacetime_engineering_materials"
    ],
    "cost": "2000000000000000000000000000000000",
    "ticks": 19400,
    "name": "维度材料",
    "description": "将材料与结构研究推进至维度工程阶段。沿前置与后续课题，追踪本分支的时代发展。",
    "branch": 2
  },
  {
    "id": "dimensional_engineering_automation",
    "tier": 6,
    "pre": [
      "spacetime_engineering_automation"
    ],
    "cost": "3000000000000000000000000000000000",
    "ticks": 19600,
    "name": "维度自动化",
    "description": "将自动化与控制研究推进至维度工程阶段。沿前置与后续课题，追踪本分支的时代发展。",
    "branch": 3
  },
  {
    "id": "dimensional_engineering_combat",
    "tier": 6,
    "pre": [
      "spacetime_engineering_combat"
    ],
    "cost": "4000000000000000000000000000000000",
    "ticks": 19800,
    "name": "维度战斗",
    "description": "将武器与防护研究推进至维度工程阶段。沿前置与后续课题，追踪本分支的时代发展。",
    "branch": 4
  },
  {
    "id": "dimensional_engineering_knowledge",
    "tier": 6,
    "pre": [
      "spacetime_engineering_knowledge"
    ],
    "cost": "5000000000000000000000000000000000",
    "ticks": 20000,
    "name": "维度知识",
    "description": "将计算与知识研究推进至维度工程阶段。沿前置与后续课题，追踪本分支的时代发展。",
    "branch": 5
  },
  {
    "id": "civilization_engineering",
    "tier": 7,
    "pre": [
      "dimensional_engineering"
    ],
    "cost": "10000000000000000000000000000000000000000",
    "ticks": 38400,
    "name": "文明工程",
    "description": "将能源转换与储存研究推进至文明工程阶段。沿前置与后续课题，追踪本分支的时代发展。",
    "branch": 1
  },
  {
    "id": "civilization_engineering_materials",
    "tier": 7,
    "pre": [
      "dimensional_engineering_materials"
    ],
    "cost": "20000000000000000000000000000000000000000",
    "ticks": 38600,
    "name": "文明材料",
    "description": "将材料与结构研究推进至文明工程阶段。沿前置与后续课题，追踪本分支的时代发展。",
    "branch": 2
  },
  {
    "id": "civilization_engineering_automation",
    "tier": 7,
    "pre": [
      "dimensional_engineering_automation"
    ],
    "cost": "30000000000000000000000000000000000000000",
    "ticks": 38800,
    "name": "文明自动化",
    "description": "将自动化与控制研究推进至文明工程阶段。沿前置与后续课题，追踪本分支的时代发展。",
    "branch": 3
  },
  {
    "id": "civilization_engineering_combat",
    "tier": 7,
    "pre": [
      "dimensional_engineering_combat"
    ],
    "cost": "40000000000000000000000000000000000000000",
    "ticks": 39000,
    "name": "文明战斗",
    "description": "将武器与防护研究推进至文明工程阶段。沿前置与后续课题，追踪本分支的时代发展。",
    "branch": 4
  },
  {
    "id": "civilization_engineering_knowledge",
    "tier": 7,
    "pre": [
      "dimensional_engineering_knowledge"
    ],
    "cost": "50000000000000000000000000000000000000000",
    "ticks": 39200,
    "name": "文明知识",
    "description": "将计算与知识研究推进至文明工程阶段。沿前置与后续课题，追踪本分支的时代发展。",
    "branch": 5
  },
  {
    "id": "ultimate_universe",
    "tier": 8,
    "pre": [
      "civilization_engineering"
    ],
    "cost": "100000000000000000000000000000000000000000000000",
    "ticks": 76800,
    "name": "终极宇宙",
    "description": "将能源转换与储存研究推进至终极宇宙阶段。沿前置与后续课题，追踪本分支的时代发展。",
    "branch": 1
  },
  {
    "id": "ultimate_universe_materials",
    "tier": 8,
    "pre": [
      "civilization_engineering_materials"
    ],
    "cost": "200000000000000000000000000000000000000000000000",
    "ticks": 77000,
    "name": "终极宇宙材料",
    "description": "将材料与结构研究推进至终极宇宙阶段。沿前置与后续课题，追踪本分支的时代发展。",
    "branch": 2
  },
  {
    "id": "ultimate_universe_automation",
    "tier": 8,
    "pre": [
      "civilization_engineering_automation"
    ],
    "cost": "300000000000000000000000000000000000000000000000",
    "ticks": 77200,
    "name": "终极宇宙自动化",
    "description": "将自动化与控制研究推进至终极宇宙阶段。沿前置与后续课题，追踪本分支的时代发展。",
    "branch": 3
  },
  {
    "id": "ultimate_universe_combat",
    "tier": 8,
    "pre": [
      "civilization_engineering_combat"
    ],
    "cost": "400000000000000000000000000000000000000000000000",
    "ticks": 77400,
    "name": "终极宇宙战斗",
    "description": "将武器与防护研究推进至终极宇宙阶段。沿前置与后续课题，追踪本分支的时代发展。",
    "branch": 4
  },
  {
    "id": "ultimate_universe_knowledge",
    "tier": 8,
    "pre": [
      "civilization_engineering_knowledge"
    ],
    "cost": "500000000000000000000000000000000000000000000000",
    "ticks": 77600,
    "name": "终极宇宙知识",
    "description": "将计算与知识研究推进至终极宇宙阶段。沿前置与后续课题，追踪本分支的时代发展。",
    "branch": 5
  }
];
