import { Ionicons } from "@expo/vector-icons";
import { createBottomTabNavigator } from "@react-navigation/bottom-tabs";
import { NavigationContainer } from "@react-navigation/native";

import { FamilyDashboardScreen } from "@/screens/FamilyDashboardScreen";
import { HistoryScreen } from "@/screens/HistoryScreen";
import { MedicationsScreen } from "@/screens/MedicationsScreen";
import { PatientHomeScreen } from "@/screens/PatientHomeScreen";
import { SettingsScreen } from "@/screens/SettingsScreen";
import { colors } from "@/theme";

export type MainTabParamList = {
  Inicio: undefined;
  Pastillas: undefined;
  Historial: undefined;
  Familia: undefined;
  Ajustes: undefined;
};

const Tab = createBottomTabNavigator<MainTabParamList>();

const iconByRoute: Record<
  keyof MainTabParamList,
  keyof typeof Ionicons.glyphMap
> = {
  Inicio: "home",
  Pastillas: "medkit",
  Historial: "heart",
  Familia: "people",
  Ajustes: "settings",
};

export function MainTabs() {
  return (
    <NavigationContainer>
      <Tab.Navigator
        screenOptions={({ route }) => ({
          headerShown: false,
          tabBarActiveTintColor: colors.primary,
          tabBarInactiveTintColor: colors.muted,
          tabBarLabelStyle: {
            fontSize: 11,
            fontWeight: "800",
          },
          tabBarStyle: {
            backgroundColor: colors.card,
            borderTopColor: colors.border,
            height: 82,
            paddingBottom: 12,
            paddingTop: 9,
          },
          tabBarIcon: ({ color, size }) => (
            <Ionicons name={iconByRoute[route.name]} size={size} color={color} />
          ),
        })}
      >
        <Tab.Screen name="Inicio" component={PatientHomeScreen} />
        <Tab.Screen name="Pastillas" component={MedicationsScreen} />
        <Tab.Screen name="Historial" component={HistoryScreen} />
        <Tab.Screen name="Familia" component={FamilyDashboardScreen} />
        <Tab.Screen name="Ajustes" component={SettingsScreen} />
      </Tab.Navigator>
    </NavigationContainer>
  );
}
