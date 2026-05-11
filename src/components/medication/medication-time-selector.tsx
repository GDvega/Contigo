import type {
  MedicationTimeParts,
  MedicationTimePeriod,
} from "@/features/medications/time-format";
import { Label } from "@/components/ui/label";

type MedicationTimeSelectorProps = {
  value: MedicationTimeParts;
  error?: string;
  idPrefix: string;
  onChange: (value: MedicationTimeParts) => void;
};

const hourOptions = Array.from({ length: 12 }, (_, index) =>
  (index + 1).toString()
);

const minuteOptions = Array.from({ length: 12 }, (_, index) =>
  (index * 5).toString().padStart(2, "0")
);

const selectClassName =
  "h-14 w-full rounded-2xl border border-input bg-[#fffdfa] px-4 text-base font-semibold outline-none transition-colors focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50";

export function MedicationTimeSelector({
  value,
  error,
  idPrefix,
  onChange,
}: MedicationTimeSelectorProps) {
  return (
    <div className="grid gap-2">
      <Label htmlFor={`${idPrefix}-hour`} className="text-base">
        Hora
      </Label>
      <div className="grid grid-cols-[1fr_1fr_1fr] gap-3">
        <select
          id={`${idPrefix}-hour`}
          value={value.hour}
          onChange={(event) =>
            onChange({
              ...value,
              hour: event.target.value,
            })
          }
          className={selectClassName}
          aria-invalid={Boolean(error)}
        >
          <option value="" disabled>
            8
          </option>
          {hourOptions.map((hour) => (
            <option key={hour} value={hour}>
              {hour}
            </option>
          ))}
        </select>

        <select
          id={`${idPrefix}-minute`}
          value={value.minute}
          onChange={(event) =>
            onChange({
              ...value,
              minute: event.target.value,
            })
          }
          className={selectClassName}
          aria-invalid={Boolean(error)}
        >
          {minuteOptions.map((minute) => (
            <option key={minute} value={minute}>
              {minute}
            </option>
          ))}
        </select>

        <select
          id={`${idPrefix}-period`}
          value={value.period}
          onChange={(event) =>
            onChange({
              ...value,
              period: event.target.value as MedicationTimePeriod,
            })
          }
          className={selectClassName}
          aria-invalid={Boolean(error)}
        >
          <option value="AM">AM</option>
          <option value="PM">PM</option>
        </select>
      </div>
      <p className="text-sm leading-6 text-muted-foreground">
        Selecciona la hora en que debe tomar el medicamento.
      </p>
      {error ? <p className="text-sm font-semibold text-rose-700">{error}</p> : null}
    </div>
  );
}
