import { Badge } from "@/components/ui/badge";
import type { PressureStatus } from "@/types";

type PressureStatusBadgeProps = {
  status: PressureStatus;
};

const statusMap = {
  NORMAL: {
    label: "Normal",
    className: "bg-emerald-100 text-emerald-800 hover:bg-emerald-100",
  },
  ELEVATED: {
    label: "Elevada",
    className: "bg-amber-100 text-amber-900 hover:bg-amber-100",
  },
  HIGH: {
    label: "Alta",
    className: "bg-rose-100 text-rose-800 hover:bg-rose-100",
  },
  CRITICAL: {
    label: "Crítica",
    className: "bg-red-700 text-white hover:bg-red-700",
  },
};

export function PressureStatusBadge({ status }: PressureStatusBadgeProps) {
  const normalizedStatus = status.toUpperCase() as keyof typeof statusMap;
  const config = statusMap[normalizedStatus] ?? statusMap.NORMAL;

  return (
    <Badge className={`rounded-full px-3 py-1.5 text-sm ${config.className}`}>
      {config.label}
    </Badge>
  );
}
